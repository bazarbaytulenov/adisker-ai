package kz.adisker.module.annual;

import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.security.UserPrincipal;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Годовой и месячные планы (ТЗ 5.8, 5.9). Годовой план с произвольными
 * разделами; мероприятия с указанным месяцем автоматически передаются
 * в соответствующий месячный план.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnnualPlanService {

    private final AnnualPlanRepository planRepo;
    private final AnnualPlanSectionRepository sectionRepo;
    private final AnnualPlanEventRepository eventRepo;
    private final MonthlyPlanRepository monthlyRepo;
    private final MonthlyPlanEventRepository monthlyEventRepo;
    private final AuditService auditService;

    // ── Годовой план ──────────────────────────────────────────────────────────

    @Transactional
    public AnnualPlanDto getOrCreatePlan(UUID branchId, String year, String language, UserPrincipal principal) {
        AnnualPlan plan = planRepo.findByBranchIdAndAcademicYearAndLanguage(branchId, year, language)
                .orElseGet(() -> planRepo.save(AnnualPlan.builder()
                        .organizationId(principal.getOrganizationId()).branchId(branchId)
                        .academicYear(year).language(language)
                        .title("Годовой план " + year).status("draft")
                        .createdBy(principal.getId()).build()));
        return toPlanDto(plan);
    }

    public List<AnnualPlanDto> listPlans(UUID branchId, UserPrincipal principal) {
        return planRepo.findByOrganizationIdAndBranchIdOrderByAcademicYearDesc(
                        principal.getOrganizationId(), branchId)
                .stream().map(this::toPlanDto).toList();
    }

    @Transactional
    public SectionDto addSection(UUID planId, String title, int sortOrder, UserPrincipal principal) {
        AnnualPlan plan = findPlan(planId, principal);
        AnnualPlanSection s = sectionRepo.save(AnnualPlanSection.builder()
                .annualPlanId(plan.getId()).title(title).sortOrder(sortOrder).build());
        return toSectionDto(s);
    }

    public List<SectionDto> listSections(UUID planId) {
        return sectionRepo.findByAnnualPlanIdOrderBySortOrder(planId).stream()
                .map(this::toSectionDto).toList();
    }

    @Transactional
    public EventDto addEvent(UUID sectionId, EventRequest req, UserPrincipal principal) {
        AnnualPlanEvent e = eventRepo.save(AnnualPlanEvent.builder()
                .sectionId(sectionId).organizationId(principal.getOrganizationId())
                .title(req.getTitle()).eventForm(req.getEventForm())
                .participants(req.getParticipants()).deadline(req.getDeadline())
                .responsible(req.getResponsible()).notes(req.getNotes())
                .month(req.getMonth()).propagated(false).build());
        return toEventDto(e);
    }

    public List<EventDto> listEvents(UUID sectionId) {
        return eventRepo.findBySectionId(sectionId).stream().map(this::toEventDto).toList();
    }

    @Transactional
    public AnnualPlanDto approvePlan(UUID planId, UserPrincipal principal) {
        AnnualPlan plan = findPlan(planId, principal);
        plan.setStatus("approved");
        plan.setApprovedBy(principal.getId());
        plan.setApprovedAt(Instant.now());
        planRepo.save(plan);
        auditService.record("APPROVE", "annual_plan", planId, null, null, "Годовой план утверждён");
        return toPlanDto(plan);
    }

    // ── Авто-передача в месячные планы (ТЗ 5.9) ──────────────────────────────────

    /**
     * Переносит все ещё не перенесённые мероприятия годового плана с указанным
     * месяцем в соответствующий месячный план (создаёт его при необходимости).
     * @return число перенесённых мероприятий
     */
    @Transactional
    public int propagateToMonthly(UUID annualPlanId, int year, int month, UserPrincipal principal) {
        AnnualPlan plan = findPlan(annualPlanId, principal);

        MonthlyPlan monthly = monthlyRepo
                .findByBranchIdAndYearAndMonthAndLanguage(plan.getBranchId(), year, month, plan.getLanguage())
                .orElseGet(() -> monthlyRepo.save(MonthlyPlan.builder()
                        .organizationId(plan.getOrganizationId()).branchId(plan.getBranchId())
                        .annualPlanId(plan.getId()).year(year).month(month)
                        .language(plan.getLanguage()).status("draft")
                        .createdBy(principal.getId()).build()));

        List<AnnualPlanEvent> events = eventRepo
                .findByOrganizationIdAndMonthAndPropagatedFalse(plan.getOrganizationId(), month);

        int count = 0;
        for (AnnualPlanEvent ev : events) {
            monthlyEventRepo.save(MonthlyPlanEvent.builder()
                    .monthlyPlanId(monthly.getId()).annualEventId(ev.getId())
                    .title(ev.getTitle()).eventForm(ev.getEventForm())
                    .participants(ev.getParticipants()).eventDate(ev.getDeadline())
                    .responsible(ev.getResponsible()).notes(ev.getNotes()).build());
            ev.setPropagated(true);
            eventRepo.save(ev);
            count++;
        }
        auditService.record("UPDATE", "monthly_plan", monthly.getId(), null, null,
                "Перенесено мероприятий из годового плана: " + count);
        return count;
    }

    public List<MonthlyPlanDto> listMonthlyPlans(UUID branchId, UserPrincipal principal) {
        return monthlyRepo.findByOrganizationIdAndBranchIdOrderByYearDescMonthDesc(
                        principal.getOrganizationId(), branchId)
                .stream().map(this::toMonthlyDto).toList();
    }

    public List<MonthlyEventDto> listMonthlyEvents(UUID monthlyPlanId) {
        return monthlyEventRepo.findByMonthlyPlanId(monthlyPlanId).stream()
                .map(this::toMonthlyEventDto).toList();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private AnnualPlan findPlan(UUID id, UserPrincipal principal) {
        return planRepo.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("AnnualPlan", id));
    }

    private AnnualPlanDto toPlanDto(AnnualPlan p) {
        return AnnualPlanDto.builder().id(p.getId()).branchId(p.getBranchId())
                .academicYear(p.getAcademicYear()).language(p.getLanguage())
                .title(p.getTitle()).status(p.getStatus()).build();
    }
    private SectionDto toSectionDto(AnnualPlanSection s) {
        return SectionDto.builder().id(s.getId()).annualPlanId(s.getAnnualPlanId())
                .title(s.getTitle()).sortOrder(s.getSortOrder()).build();
    }
    private EventDto toEventDto(AnnualPlanEvent e) {
        return EventDto.builder().id(e.getId()).sectionId(e.getSectionId()).title(e.getTitle())
                .eventForm(e.getEventForm()).participants(e.getParticipants()).deadline(e.getDeadline())
                .responsible(e.getResponsible()).notes(e.getNotes()).month(e.getMonth())
                .propagated(e.isPropagated()).build();
    }
    private MonthlyPlanDto toMonthlyDto(MonthlyPlan m) {
        return MonthlyPlanDto.builder().id(m.getId()).branchId(m.getBranchId())
                .year(m.getYear()).month(m.getMonth()).language(m.getLanguage()).status(m.getStatus()).build();
    }
    private MonthlyEventDto toMonthlyEventDto(MonthlyPlanEvent e) {
        return MonthlyEventDto.builder().id(e.getId()).monthlyPlanId(e.getMonthlyPlanId())
                .title(e.getTitle()).eventForm(e.getEventForm()).eventDate(e.getEventDate())
                .responsible(e.getResponsible()).notes(e.getNotes()).build();
    }

    // ── DTO ──────────────────────────────────────────────────────────────────
    @Data @Builder public static class AnnualPlanDto {
        private UUID id, branchId; private String academicYear, language, title, status;
    }
    @Data @Builder public static class SectionDto {
        private UUID id, annualPlanId; private String title; private int sortOrder;
    }
    @Data public static class EventRequest {
        private String title, eventForm, participants, responsible, notes;
        private java.time.LocalDate deadline; private Integer month;
    }
    @Data @Builder public static class EventDto {
        private UUID id, sectionId; private String title, eventForm, participants, responsible, notes;
        private java.time.LocalDate deadline; private Integer month; private boolean propagated;
    }
    @Data @Builder public static class MonthlyPlanDto {
        private UUID id, branchId; private int year, month; private String language, status;
    }
    @Data @Builder public static class MonthlyEventDto {
        private UUID id, monthlyPlanId; private String title, eventForm, responsible, notes;
        private java.time.LocalDate eventDate;
    }
}
