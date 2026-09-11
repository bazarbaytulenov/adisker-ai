package kz.adisker.module.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.adisker.common.exception.BusinessException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.module.branch.BranchRepository;
import kz.adisker.module.child.ChildRepository;
import kz.adisker.module.group.GroupRepository;
import kz.adisker.module.organization.OrganizationRepository;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Экспорт и восстановление данных организации (ТЗ 5.28, п.15).
 * Экспорт — снимок ключевых справочных данных в JSON.
 * Все операции фиксируются в журнале аудита (журнал восстановления).
 */
@Service
@RequiredArgsConstructor
public class BackupService {

    private final OrganizationRepository orgRepo;
    private final BranchRepository branchRepo;
    private final GroupRepository groupRepo;
    private final ChildRepository childRepo;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /** Экспорт организации в JSON-строку. */
    public String exportOrganization(UUID organizationId, UserPrincipal principal) {
        if (!organizationId.equals(principal.getOrganizationId())
                && !"SYSTEM_ADMIN".equals(principal.getRoleCode())) {
            throw new BusinessException("Экспорт доступен только для своей организации");
        }
        var org = orgRepo.findById(organizationId)
                .orElseThrow(() -> new BusinessException("Организация не найдена"));

        Map<String, Object> dump = new LinkedHashMap<>();
        dump.put("exportedAt", Instant.now().toString());
        dump.put("exportedBy", principal.getEmail());
        dump.put("formatVersion", 1);

        Map<String, Object> orgMap = new LinkedHashMap<>();
        orgMap.put("id", org.getId());
        orgMap.put("name", org.getName());
        orgMap.put("legalName", org.getLegalName());
        orgMap.put("bin", org.getBin());
        orgMap.put("address", org.getAddress());
        dump.put("organization", orgMap);

        dump.put("branches", branchRepo.findByOrganizationIdAndDeletedFalseAndActiveTrue(organizationId)
                .stream().map(b -> Map.of(
                        "id", String.valueOf(b.getId()),
                        "name", String.valueOf(b.getName()))).toList());
        dump.put("groups", groupRepo.findByOrganizationIdAndDeletedFalseAndActiveTrue(organizationId)
                .stream().map(g -> Map.of(
                        "id", String.valueOf(g.getId()),
                        "name", String.valueOf(g.getName()),
                        "branchId", String.valueOf(g.getBranchId()))).toList());
        dump.put("children", childRepo
                .findByOrganizationIdAndDeletedFalse(organizationId, Pageable.unpaged()).getContent()
                .stream().map(c -> Map.of(
                        "id", String.valueOf(c.getId()),
                        "lastName", String.valueOf(c.getLastName()),
                        "firstName", String.valueOf(c.getFirstName()),
                        "status", String.valueOf(c.getStatus()))).toList());

        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dump);
            auditService.record("EXPORT", "organization_backup", organizationId, null, null,
                    "Экспорт резервной копии организации");
            return json;
        } catch (Exception e) {
            throw new BusinessException("Не удалось сформировать резервную копию: " + e.getMessage());
        }
    }

    /**
     * Регистрация факта восстановления из резервной копии.
     * Само применение данных выполняется контролируемо администратором;
     * здесь фиксируется событие в журнале восстановления (ТЗ п.15).
     */
    public void registerRestore(UUID organizationId, String note, UserPrincipal principal) {
        auditService.record("RESTORE", "organization_backup", organizationId, null, null,
                "Восстановление из резервной копии: " + (note != null ? note : ""));
    }
}
