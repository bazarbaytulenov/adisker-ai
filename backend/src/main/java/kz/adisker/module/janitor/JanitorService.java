package kz.adisker.module.janitor;

import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Хозяйственный модуль завхоза (ТЗ 5.23): инструктажи ТБ, несчастные случаи,
 * пожарная безопасность, инвентарь.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JanitorService {

    private final JanitorRecordRepository repo;
    private final AuditService auditService;

    @Transactional
    public JanitorRecord create(RecordRequest req, UserPrincipal principal) {
        JanitorRecord r = JanitorRecord.builder()
                .organizationId(principal.getOrganizationId())
                .branchId(req.getBranchId())
                .recordType(req.getRecordType())
                .title(req.getTitle())
                .recordDate(req.getRecordDate() != null ? req.getRecordDate() : LocalDate.now())
                .responsible(req.getResponsible())
                .details(req.getDetails() != null ? req.getDetails() : "{}")
                .notes(req.getNotes())
                .status("active")
                .createdBy(principal.getId())
                .deleted(false)
                .build();
        JanitorRecord saved = repo.save(r);
        auditService.record("CREATE", "janitor_record", saved.getId(), null, null,
                "Запись завхоза: " + saved.getRecordType());
        return saved;
    }

    @Transactional
    public JanitorRecord update(UUID id, RecordRequest req, UserPrincipal principal) {
        JanitorRecord r = find(id, principal);
        if (req.getTitle() != null) r.setTitle(req.getTitle());
        if (req.getRecordDate() != null) r.setRecordDate(req.getRecordDate());
        if (req.getResponsible() != null) r.setResponsible(req.getResponsible());
        if (req.getDetails() != null) r.setDetails(req.getDetails());
        if (req.getNotes() != null) r.setNotes(req.getNotes());
        if (req.getStatus() != null) r.setStatus(req.getStatus());
        return repo.save(r);
    }

    public List<JanitorRecord> list(UUID branchId, String type, UserPrincipal principal) {
        if (type != null && !type.isBlank()) {
            return repo.findByOrganizationIdAndRecordTypeAndDeletedFalseOrderByRecordDateDesc(
                    principal.getOrganizationId(), type);
        }
        return repo.findByOrganizationIdAndBranchIdAndDeletedFalseOrderByRecordDateDesc(
                principal.getOrganizationId(), branchId);
    }

    public JanitorRecord get(UUID id, UserPrincipal principal) {
        return find(id, principal);
    }

    @Transactional
    public void delete(UUID id, UserPrincipal principal) {
        JanitorRecord r = find(id, principal);
        r.setDeleted(true);
        r.setDeletedAt(Instant.now());
        repo.save(r);
        auditService.record("DELETE", "janitor_record", id, null, null, "Запись завхоза удалена");
    }

    private JanitorRecord find(UUID id, UserPrincipal principal) {
        return repo.findByIdAndOrganizationIdAndDeletedFalse(id, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("JanitorRecord", id));
    }

    @Data
    public static class RecordRequest {
        private UUID branchId;
        private String recordType; // safety_briefing / incident / fire_safety / inventory
        private String title;
        private LocalDate recordDate;
        private String responsible;
        private String details; // JSON
        private String notes;
        private String status;
    }
}
