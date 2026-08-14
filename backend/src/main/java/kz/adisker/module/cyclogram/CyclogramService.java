package kz.adisker.module.cyclogram;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CyclogramService {

    private final CyclogramRepository repo;
    private final ObjectMapper objectMapper;

    // ── Список ────────────────────────────────────────────────────────────────

    public List<CyclogramDto> list(UUID organizationId, UUID branchId, UUID groupId) {
        List<Cyclogram> items;
        if (groupId != null) {
            items = repo.findByOrganizationIdAndGroupIdAndDeletedFalse(organizationId, groupId);
        } else if (branchId != null) {
            items = repo.findByOrganizationIdAndBranchIdAndDeletedFalse(organizationId, branchId);
        } else {
            items = repo.findByOrganizationIdAndDeletedFalse(organizationId);
        }
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Получить по ID ────────────────────────────────────────────────────────

    public CyclogramDto get(UUID id, UUID organizationId) {
        Cyclogram c = findAndCheck(id, organizationId);
        return toDto(c);
    }

    // ── Создать ───────────────────────────────────────────────────────────────

    @Transactional
    public CyclogramDto create(CyclogramRequest req, UserPrincipal principal) {
        Cyclogram c = Cyclogram.builder()
                .organizationId(req.getOrganizationId())
                .branchId(req.getBranchId())
                .groupId(req.getGroupId())
                .academicYear(req.getAcademicYear())
                .month(req.getMonth())
                .week(req.getWeek())
                .language(req.getLanguage() != null ? req.getLanguage() : "ru")
                .content(req.getContent())
                .status("draft")
                .generatedByAi(false)
                .build();
        return toDto(repo.save(c));
    }

    // ── Обновить ──────────────────────────────────────────────────────────────

    @Transactional
    public CyclogramDto update(UUID id, CyclogramRequest req, UserPrincipal principal) {
        Cyclogram c = findAndCheck(id, req.getOrganizationId());
        if (req.getContent() != null)      c.setContent(req.getContent());
        if (req.getAcademicYear() != null) c.setAcademicYear(req.getAcademicYear());
        if (req.getLanguage() != null)     c.setLanguage(req.getLanguage());
        if (req.getMonth() > 0)            c.setMonth(req.getMonth());
        if (req.getWeek() > 0)             c.setWeek(req.getWeek());
        if (req.getStatus() != null)       c.setStatus(req.getStatus());
        return toDto(repo.save(c));
    }

    // ── Утвердить ─────────────────────────────────────────────────────────────

    @Transactional
    public CyclogramDto approve(UUID id, UUID organizationId) {
        Cyclogram c = findAndCheck(id, organizationId);
        c.setStatus("approved");
        return toDto(repo.save(c));
    }

    // ── Удалить (soft delete через BaseEntity) ───────────────────────────────

    @Transactional
    public void delete(UUID id, UUID organizationId) {
        Cyclogram c = findAndCheck(id, organizationId);
        c.setDeleted(true);
        repo.save(c);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Cyclogram findAndCheck(UUID id, UUID organizationId) {
        Cyclogram c = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cyclogram", id));
        if (!c.getOrganizationId().equals(organizationId)) {
            throw new kz.adisker.common.exception.AccessDeniedException("Нет доступа к этой циклограмме");
        }
        return c;
    }

    private CyclogramDto toDto(Cyclogram c) {
        return CyclogramDto.builder()
                .id(c.getId())
                .organizationId(c.getOrganizationId())
                .branchId(c.getBranchId())
                .groupId(c.getGroupId())
                .academicYear(c.getAcademicYear())
                .month(c.getMonth())
                .week(c.getWeek())
                .language(c.getLanguage())
                .content(c.getContent())
                .status(c.getStatus())
                .generatedByAi(c.isGeneratedByAi())
                .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
                .updatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null)
                .build();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @Data
    @lombok.Builder
    public static class CyclogramDto {
        private UUID id;
        private UUID organizationId;
        private UUID branchId;
        private UUID groupId;
        private String academicYear;
        private int month;
        private int week;
        private String language;
        private String content; // JSONB as string
        private String status;
        private boolean generatedByAi;
        private String createdAt;
        private String updatedAt;
    }
}
