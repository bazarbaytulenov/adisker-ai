package kz.adisker.module.routine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Режим дня (ТЗ 5.18): формирование методистом по группе/году/языку,
 * шаблон "время — режимный момент", публикация, экспорт Word.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository repo;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    /** Шаблон режима дня по умолчанию. */
    private static final String DEFAULT_TEMPLATE = """
            [{"time":"07:00-08:00","activity":"Приём детей, утренний фильтр"},
             {"time":"08:00-08:30","activity":"Утренняя гимнастика"},
             {"time":"08:30-09:00","activity":"Завтрак"},
             {"time":"09:00-10:40","activity":"Организованная деятельность"},
             {"time":"10:40-12:10","activity":"Прогулка"},
             {"time":"12:10-12:40","activity":"Обед"},
             {"time":"12:40-15:00","activity":"Дневной сон"},
             {"time":"15:00-15:30","activity":"Постепенный подъём, закаливание"},
             {"time":"15:30-16:00","activity":"Полдник"},
             {"time":"16:00-17:30","activity":"Игры, самостоятельная деятельность"},
             {"time":"17:30-19:00","activity":"Прогулка, уход домой"}]""";

    @Transactional
    public Routine getOrCreate(UUID branchId, UUID groupId, String year, String language,
                               UserPrincipal principal) {
        return repo.findByGroupIdAndAcademicYearAndLanguage(groupId, year, language)
                .orElseGet(() -> repo.save(Routine.builder()
                        .organizationId(principal.getOrganizationId()).branchId(branchId)
                        .groupId(groupId).academicYear(year).language(language)
                        .items("[]").published(false).createdBy(principal.getId()).build()));
    }

    @Transactional
    public Routine save(UUID id, RoutineRequest req, UserPrincipal principal) {
        Routine r = find(id, principal);
        if (r.isPublished()) throw new BusinessException("Опубликованный режим дня редактировать нельзя");
        if (req.getApprovalInfo() != null) r.setApprovalInfo(req.getApprovalInfo());
        if (req.getGroupName() != null) r.setGroupName(req.getGroupName());
        if (req.getItems() != null) r.setItems(req.getItems());
        r.setUpdatedBy(principal.getId());
        return repo.save(r);
    }

    /** Заполнить режим дня по стандартному шаблону. */
    @Transactional
    public Routine applyTemplate(UUID id, UserPrincipal principal) {
        Routine r = find(id, principal);
        if (r.isPublished()) throw new BusinessException("Опубликованный режим дня редактировать нельзя");
        r.setItems(DEFAULT_TEMPLATE.replaceAll("\\s+", " "));
        r.setUpdatedBy(principal.getId());
        return repo.save(r);
    }

    @Transactional
    public Routine publish(UUID id, UserPrincipal principal) {
        Routine r = find(id, principal);
        r.setPublished(true);
        r.setPublishedAt(Instant.now());
        Routine saved = repo.save(r);
        auditService.record("UPDATE", "routine", id, null, null, "Режим дня опубликован");
        return saved;
    }

    @Transactional
    public Routine unpublish(UUID id, UserPrincipal principal) {
        Routine r = find(id, principal);
        r.setPublished(false);
        r.setPublishedAt(null);
        return repo.save(r);
    }

    public List<Routine> listByGroup(UUID groupId) {
        return repo.findByGroupIdOrderByAcademicYearDesc(groupId);
    }

    /** Экспорт режима дня в Word (таблица «Время — Режимный момент»). */
    public byte[] exportWord(UUID id, UserPrincipal principal) {
        Routine r = find(id, principal);
        String title = "РЕЖИМ ДНЯ" + (r.getGroupName() != null ? " — " + r.getGroupName() : "")
                + " (" + r.getLanguage() + ")";
        List<String> paras = new ArrayList<>();
        if (r.getApprovalInfo() != null) paras.add(r.getApprovalInfo());

        List<String> headers = List.of("Время", "Режимный момент");
        List<List<String>> rows = new ArrayList<>();
        try {
            JsonNode arr = objectMapper.readTree(r.getItems() != null ? r.getItems() : "[]");
            for (JsonNode item : arr) {
                rows.add(List.of(
                        item.path("time").asText(""),
                        item.path("activity").asText("")));
            }
        } catch (Exception ignore) { /* пустой список */ }

        byte[] doc = WordExporter.toDocx(title, paras, headers, rows);
        auditService.record("EXPORT", "routine", id, null, null, "Экспорт режима дня (Word)");
        return doc;
    }

    private Routine find(UUID id, UserPrincipal principal) {
        return repo.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Routine", id));
    }

    @Data
    public static class RoutineRequest {
        private String approvalInfo;
        private String groupName;
        private String items; // JSON-массив [{time, activity}]
    }
}
