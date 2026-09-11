package kz.adisker.module.plan;

import kz.adisker.common.RoleCode;
import kz.adisker.common.exception.AccessDeniedException;
import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final ProspectivePlanRepository planRepo;
    private final PlanSectionRepository sectionRepo;
    private final PlanLockRepository lockRepo;
    private final PlanVersionRepository versionRepo;
    private final SpecialistGroupAssignmentRepository assignmentRepo;
    private final AuditService auditService;

    // ── Домены (графы) образовательных областей ────────────────────────────────
    static final String DOMAIN_KAZ      = "kaz_language"; // Қазақ тілі
    static final String DOMAIN_MUSIC    = "music";        // Музыка
    static final String DOMAIN_PHYSICAL = "physical";     // Физическая культура
    static final String DOMAIN_EDUCATOR = "educator";     // области воспитателя

    /** Единственная графа, которую специалист вправе редактировать (иначе null). */
    private static final Map<String, String> ROLE_DOMAIN = Map.of(
            RoleCode.KAZ_TEACHER,   DOMAIN_KAZ,
            RoleCode.MUSIC_TEACHER, DOMAIN_MUSIC,
            RoleCode.PE_INSTRUCTOR, DOMAIN_PHYSICAL
    );

    // ── Перспективный план ────────────────────────────────────────────────────

    /** Получить или создать план на неделю */
    @Transactional
    public PlanDto getOrCreate(UUID orgId, UUID branchId, UUID groupId,
                               String year, int month, int week, String language,
                               UserPrincipal principal) {
        ProspectivePlan plan = planRepo
                .findByGroupIdAndAcademicYearAndMonthAndWeekAndLanguageAndDeletedFalse(groupId, year, month, week, language)
                .orElseGet(() -> planRepo.save(ProspectivePlan.builder()
                        .organizationId(orgId).branchId(branchId).groupId(groupId)
                        .academicYear(year).month(month).week(week).language(language)
                        .overallStatus("draft").fillPct(BigDecimal.ZERO)
                        .build()));
        return toDto(plan);
    }

    /** Список планов группы за учебный год */
    public List<PlanDto> listByGroup(UUID orgId, UUID groupId, String year, String language) {
        return planRepo.findByGroupIdAndAcademicYearAndLanguageAndDeletedFalse(groupId, year, language)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Секции плана ──────────────────────────────────────────────────────────

    public List<SectionDto> getSections(UUID planId) {
        return sectionRepo.findByPlanIdAndDeletedFalseOrderBySortOrder(planId)
                .stream().map(this::toSectionDto).collect(Collectors.toList());
    }

    /** История версий секции (новые сверху). */
    public List<VersionDto> getSectionHistory(UUID sectionId) {
        return versionRepo.findBySectionIdOrderByVersionDesc(sectionId)
                .stream().map(this::toVersionDto).collect(Collectors.toList());
    }

    @Transactional
    public SectionDto saveSection(UUID planId, UUID orgId, SectionRequest req, UserPrincipal principal) {
        ProspectivePlan plan = planRepo.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("ProspectivePlan", planId));

        // Проверка прав: специалист редактирует только свою графу и только своей группы
        assertCanEditSection(plan, req.getDomain(), principal);

        PlanSection section = sectionRepo
                .findByPlanIdAndDomainAndDeletedFalse(planId, req.getDomain())
                .orElseGet(() -> {
                    PlanSection s = new PlanSection();
                    s.setPlanId(planId);
                    s.setOrganizationId(orgId);
                    s.setDomain(req.getDomain());
                    s.setVersion(1);
                    return s;
                });

        // Утверждённую графу редактировать нельзя — только после возврата методистом
        if ("approved".equals(section.getStatus())) {
            throw new BusinessException("Графа утверждена и доступна только для просмотра. "
                    + "Для изменений методист должен вернуть её на доработку.");
        }

        // Сохраняем версию, если контент изменился
        if (section.getId() != null && req.getContent() != null
                && !req.getContent().equals(section.getContent())) {
            lockRepo.findBySectionId(section.getId()).ifPresent(lock -> {
                if (!lock.getLockedBy().equals(principal.getId())
                        && lock.getExpiresAt().isAfter(Instant.now()))
                    throw new BusinessException("Секция заблокирована другим пользователем");
            });
            versionRepo.save(PlanVersion.builder()
                    .sectionId(section.getId())
                    .version(section.getVersion())
                    .content(section.getContent())
                    .objectives(section.getObjectives())
                    .materials(section.getMaterials())
                    .changedBy(principal.getId())
                    .changedAt(Instant.now())
                    .build());
            section.setVersion(section.getVersion() + 1);
        }

        if (req.getContent() != null)       section.setContent(req.getContent());
        if (req.getObjectives() != null)    section.setObjectives(req.getObjectives());
        if (req.getMaterials() != null)     section.setMaterials(req.getMaterials());
        if (req.getDomainNameRu() != null)  section.setDomainNameRu(req.getDomainNameRu());
        if (req.getDomainNameKk() != null)  section.setDomainNameKk(req.getDomainNameKk());
        if (req.getOwnerRole() != null)     section.setOwnerRole(req.getOwnerRole());
        if (req.getSortOrder() >= 0)        section.setSortOrder(req.getSortOrder());
        section.setUpdatedBy(principal.getId());

        PlanSection saved = sectionRepo.save(section);
        recalcFillPct(planId);
        return toSectionDto(saved);
    }

    @Transactional
    public SectionDto submitSection(UUID sectionId, UUID orgId, UserPrincipal principal) {
        PlanSection s = findSection(sectionId);
        ProspectivePlan plan = planRepo.findById(s.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("ProspectivePlan", s.getPlanId()));
        assertCanEditSection(plan, s.getDomain(), principal);
        s.setStatus("review");
        s.setSubmittedAt(Instant.now());
        s.setUpdatedBy(principal.getId());
        return toSectionDto(sectionRepo.save(s));
    }

    @Transactional
    public SectionDto approveSection(UUID sectionId, UUID orgId, UserPrincipal principal) {
        PlanSection s = findSection(sectionId);
        s.setStatus("approved");
        s.setApprovedAt(Instant.now());
        s.setApprovedBy(principal.getId());
        s.setUpdatedBy(principal.getId());
        PlanSection saved = sectionRepo.save(s);
        recalcFillPct(s.getPlanId());
        auditService.record("APPROVE", "plan_section", saved.getId(), null, null,
                "Утверждена графа: " + saved.getDomain());
        return toSectionDto(saved);
    }

    @Transactional
    public SectionDto returnSection(UUID sectionId, String comment, UserPrincipal principal) {
        PlanSection s = findSection(sectionId);
        s.setStatus("returned");
        s.setReturnedAt(Instant.now());
        s.setReturnComment(comment);
        s.setUpdatedBy(principal.getId());
        PlanSection saved = sectionRepo.save(s);
        auditService.record("REJECT", "plan_section", saved.getId(), null, null,
                "Возврат на доработку: " + comment);
        return toSectionDto(saved);
    }

    // ── Блокировка секции ─────────────────────────────────────────────────────

    @Transactional
    public void lockSection(UUID sectionId, UserPrincipal principal) {
        lockRepo.findBySectionId(sectionId).ifPresent(existing -> {
            if (!existing.getLockedBy().equals(principal.getId())
                    && existing.getExpiresAt().isAfter(Instant.now())) {
                throw new BusinessException("Секция заблокирована другим пользователем");
            }
            lockRepo.delete(existing);
        });
        lockRepo.save(PlanLock.builder()
                .sectionId(sectionId)
                .lockedBy(principal.getId())
                .lockedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300)) // 5 минут
                .build());
    }

    @Transactional
    public void unlockSection(UUID sectionId, UserPrincipal principal) {
        lockRepo.findBySectionId(sectionId).ifPresent(lock -> {
            if (lock.getLockedBy().equals(principal.getId())) lockRepo.delete(lock);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Проверяет, вправе ли пользователь редактировать графу {@code domain} плана.
     * Правила (ТЗ 3.3, 5.11):
     *  - METHODIST / DIRECTOR / SYSTEM_ADMIN — все графы всех групп;
     *  - EDUCATOR — все графы, кроме зарезервированных за специалистами;
     *  - KAZ_TEACHER / MUSIC_TEACHER / PE_INSTRUCTOR — только свою графу и только
     *    в закреплённой за ним группе;
     *  - прочие роли — запрещено.
     */
    void assertCanEditSection(ProspectivePlan plan, String domain, UserPrincipal principal) {
        String role = principal.getRoleCode();

        // Изоляция арендатора: план чужой организации недоступен
        if (!plan.getOrganizationId().equals(principal.getOrganizationId())) {
            throw new AccessDeniedException("План принадлежит другой организации");
        }

        // Методист / руководитель / админ — полный доступ ко всем графам
        if (RoleCode.METHODIST.equals(role)
                || RoleCode.DIRECTOR.equals(role)
                || RoleCode.SYSTEM_ADMIN.equals(role)) {
            return;
        }

        // Специалисты — только своя графа и только закреплённая группа
        String allowedDomain = ROLE_DOMAIN.get(role);
        if (allowedDomain != null) {
            if (!allowedDomain.equals(domain)) {
                throw new AccessDeniedException(
                        "Специалист может редактировать только свою образовательную область");
            }
            if (!assignmentRepo.existsBySpecialistIdAndGroupId(principal.getId(), plan.getGroupId())) {
                throw new AccessDeniedException("Группа не закреплена за специалистом");
            }
            return;
        }

        // Воспитатель — все графы, кроме зарезервированных за специалистами
        if (RoleCode.EDUCATOR.equals(role)) {
            if (ROLE_DOMAIN.containsValue(domain)) {
                throw new AccessDeniedException(
                        "Эта графа закреплена за специалистом и недоступна воспитателю для редактирования");
            }
            return;
        }

        throw new AccessDeniedException("Недостаточно прав для редактирования перспективного плана");
    }

    private PlanSection findSection(UUID id) {
        return sectionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlanSection", id));
    }

    private void recalcFillPct(UUID planId) {
        List<PlanSection> sections = sectionRepo.findByPlanIdAndDeletedFalseOrderBySortOrder(planId);
        if (sections.isEmpty()) return;
        long approved = sections.stream().filter(s -> "approved".equals(s.getStatus())).count();
        BigDecimal pct = BigDecimal.valueOf(approved * 100.0 / sections.size());
        planRepo.findById(planId).ifPresent(p -> {
            p.setFillPct(pct);
            if (approved == sections.size()) p.setOverallStatus("approved");
            planRepo.save(p);
        });
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    private PlanDto toDto(ProspectivePlan p) {
        return PlanDto.builder()
                .id(p.getId()).organizationId(p.getOrganizationId())
                .branchId(p.getBranchId()).groupId(p.getGroupId())
                .academicYear(p.getAcademicYear()).month(p.getMonth()).week(p.getWeek())
                .theme(p.getTheme()).language(p.getLanguage())
                .overallStatus(p.getOverallStatus()).fillPct(p.getFillPct())
                .build();
    }

    private VersionDto toVersionDto(PlanVersion v) {
        return VersionDto.builder()
                .id(v.getId()).sectionId(v.getSectionId()).version(v.getVersion())
                .content(v.getContent()).objectives(v.getObjectives()).materials(v.getMaterials())
                .changedBy(v.getChangedBy()).changedAt(v.getChangedAt())
                .changeComment(v.getChangeComment())
                .build();
    }

    private SectionDto toSectionDto(PlanSection s) {
        return SectionDto.builder()
                .id(s.getId()).planId(s.getPlanId()).domain(s.getDomain())
                .domainNameRu(s.getDomainNameRu()).domainNameKk(s.getDomainNameKk())
                .ownerRole(s.getOwnerRole()).ownerUserId(s.getOwnerUserId())
                .content(s.getContent()).objectives(s.getObjectives()).materials(s.getMaterials())
                .status(s.getStatus()).version(s.getVersion()).sortOrder(s.getSortOrder())
                .returnComment(s.getReturnComment())
                .build();
    }

    @Data @lombok.Builder
    public static class PlanDto {
        private UUID id, organizationId, branchId, groupId;
        private String academicYear, theme, language, overallStatus;
        private int month, week;
        private BigDecimal fillPct;
    }

    @Data @lombok.Builder
    public static class SectionDto {
        private UUID id, planId, ownerUserId;
        private String domain, domainNameRu, domainNameKk, ownerRole;
        private String content, objectives, materials, status, returnComment;
        private int version, sortOrder;
    }

    @Data @lombok.Builder
    public static class VersionDto {
        private UUID id, sectionId, changedBy;
        private int version;
        private String content, objectives, materials, changeComment;
        private java.time.Instant changedAt;
    }
}
