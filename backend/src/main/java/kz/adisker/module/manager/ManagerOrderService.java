package kz.adisker.module.manager;

import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.module.export.WordExporter;
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
 * Приказы руководителя (ТЗ 5.24): CRUD, подписание, экспорт Word.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerOrderService {

    private final ManagerOrderRepository repo;
    private final AuditService auditService;

    @Transactional
    public ManagerOrder create(OrderRequest req, UserPrincipal principal) {
        ManagerOrder o = ManagerOrder.builder()
                .organizationId(principal.getOrganizationId())
                .branchId(req.getBranchId())
                .orderType(req.getOrderType())
                .orderNumber(req.getOrderNumber())
                .orderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now())
                .subject(req.getSubject())
                .content(req.getContent())
                .relatedChildId(req.getRelatedChildId())
                .relatedUserId(req.getRelatedUserId())
                .language(req.getLanguage() != null ? req.getLanguage() : "ru")
                .status("draft")
                .createdBy(principal.getId())
                .deleted(false)
                .build();
        ManagerOrder saved = repo.save(o);
        auditService.record("CREATE", "manager_order", saved.getId(), null, null,
                "Приказ " + saved.getOrderType() + " №" + saved.getOrderNumber());
        return saved;
    }

    @Transactional
    public ManagerOrder update(UUID id, OrderRequest req, UserPrincipal principal) {
        ManagerOrder o = find(id, principal);
        if ("signed".equals(o.getStatus())) throw new BusinessException("Подписанный приказ изменять нельзя");
        if (req.getOrderNumber() != null) o.setOrderNumber(req.getOrderNumber());
        if (req.getOrderDate() != null) o.setOrderDate(req.getOrderDate());
        if (req.getSubject() != null) o.setSubject(req.getSubject());
        if (req.getContent() != null) o.setContent(req.getContent());
        if (req.getLanguage() != null) o.setLanguage(req.getLanguage());
        return repo.save(o);
    }

    @Transactional
    public ManagerOrder sign(UUID id, UserPrincipal principal) {
        ManagerOrder o = find(id, principal);
        o.setStatus("signed");
        o.setSignedBy(principal.getId());
        o.setSignedAt(Instant.now());
        ManagerOrder saved = repo.save(o);
        auditService.record("APPROVE", "manager_order", id, null, null, "Приказ подписан");
        return saved;
    }

    public List<ManagerOrder> list(UUID branchId, String type, UserPrincipal principal) {
        if (type != null && !type.isBlank()) {
            return repo.findByOrganizationIdAndOrderTypeAndDeletedFalseOrderByOrderDateDesc(
                    principal.getOrganizationId(), type);
        }
        return repo.findByOrganizationIdAndBranchIdAndDeletedFalseOrderByOrderDateDesc(
                principal.getOrganizationId(), branchId);
    }

    public ManagerOrder get(UUID id, UserPrincipal principal) {
        return find(id, principal);
    }

    @Transactional
    public void delete(UUID id, UserPrincipal principal) {
        ManagerOrder o = find(id, principal);
        if ("signed".equals(o.getStatus())) throw new BusinessException("Подписанный приказ удалять нельзя");
        o.setDeleted(true);
        repo.save(o);
        auditService.record("DELETE", "manager_order", id, null, null, "Приказ удалён");
    }

    public byte[] exportWord(UUID id, UserPrincipal principal) {
        ManagerOrder o = find(id, principal);
        String title = "ПРИКАЗ № " + (o.getOrderNumber() != null ? o.getOrderNumber() : "")
                + " от " + o.getOrderDate();
        var paras = List.of(
                typeName(o.getOrderType()),
                "",
                o.getSubject() != null ? o.getSubject() : "",
                "",
                o.getContent() != null ? o.getContent() : "",
                "",
                "signed".equals(o.getStatus()) ? "Подписано." : "Проект приказа.");
        byte[] doc = WordExporter.toDocx(title, paras, null, null);
        auditService.record("EXPORT", "manager_order", id, null, null, "Экспорт приказа (Word)");
        return doc;
    }

    private ManagerOrder find(UUID id, UserPrincipal principal) {
        return repo.findByIdAndOrganizationIdAndDeletedFalse(id, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("ManagerOrder", id));
    }

    private String typeName(String type) {
        return switch (type) {
            case "child_admission"  -> "О приёме ребёнка";
            case "child_transfer"   -> "О переводе ребёнка";
            case "child_discharge"  -> "О выбытии ребёнка";
            case "child_graduation" -> "О выпуске ребёнка";
            case "staff_hire"       -> "О приёме сотрудника";
            case "staff_transfer"   -> "О переводе сотрудника";
            case "staff_dismiss"    -> "Об увольнении сотрудника";
            default                 -> type;
        };
    }

    @Data
    public static class OrderRequest {
        private UUID branchId;
        private String orderType;
        private String orderNumber;
        private LocalDate orderDate;
        private String subject;
        private String content;
        private UUID relatedChildId;
        private UUID relatedUserId;
        private String language;
    }
}
