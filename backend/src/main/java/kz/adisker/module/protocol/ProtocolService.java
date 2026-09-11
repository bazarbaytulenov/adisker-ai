package kz.adisker.module.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.module.export.WordExporter;
import kz.adisker.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Протоколы советов (ТЗ 5.10): CRUD + печать/экспорт Word.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProtocolService {

    private final ProtocolRepository repo;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public Protocol create(ProtocolRequest req, UserPrincipal principal) {
        Protocol p = Protocol.builder()
                .organizationId(principal.getOrganizationId())
                .branchId(req.getBranchId())
                .protocolType(req.getProtocolType())
                .number(req.getNumber())
                .protocolDate(req.getProtocolDate() != null ? req.getProtocolDate() : LocalDate.now())
                .language(req.getLanguage() != null ? req.getLanguage() : "ru")
                .chairman(req.getChairman()).secretary(req.getSecretary())
                .attendees(req.getAttendees()).agenda(req.getAgenda())
                .heard(req.getHeard()).speakers(req.getSpeakers()).decisions(req.getDecisions())
                .status("draft").createdBy(principal.getId()).updatedBy(principal.getId())
                .build();
        Protocol saved = repo.save(p);
        auditService.record("CREATE", "protocol", saved.getId(), null, null,
                "Протокол " + saved.getProtocolType() + " №" + saved.getNumber());
        return saved;
    }

    @Transactional
    public Protocol update(UUID id, ProtocolRequest req, UserPrincipal principal) {
        Protocol p = find(id, principal);
        if (req.getNumber() != null) p.setNumber(req.getNumber());
        if (req.getProtocolDate() != null) p.setProtocolDate(req.getProtocolDate());
        if (req.getChairman() != null) p.setChairman(req.getChairman());
        if (req.getSecretary() != null) p.setSecretary(req.getSecretary());
        if (req.getAttendees() != null) p.setAttendees(req.getAttendees());
        if (req.getAgenda() != null) p.setAgenda(req.getAgenda());
        if (req.getHeard() != null) p.setHeard(req.getHeard());
        if (req.getSpeakers() != null) p.setSpeakers(req.getSpeakers());
        if (req.getDecisions() != null) p.setDecisions(req.getDecisions());
        p.setUpdatedBy(principal.getId());
        return repo.save(p);
    }

    public List<Protocol> list(UUID branchId, String type, UserPrincipal principal) {
        if (type != null && !type.isBlank()) {
            return repo.findByOrganizationIdAndProtocolTypeOrderByProtocolDateDesc(
                    principal.getOrganizationId(), type);
        }
        return repo.findByOrganizationIdAndBranchIdOrderByProtocolDateDesc(
                principal.getOrganizationId(), branchId);
    }

    public Protocol get(UUID id, UserPrincipal principal) {
        return find(id, principal);
    }

    @Transactional
    public void delete(UUID id, UserPrincipal principal) {
        Protocol p = find(id, principal);
        repo.delete(p);
        auditService.record("DELETE", "protocol", id, null, null, "Протокол удалён");
    }

    /** Экспорт протокола в Word (DOCX). */
    public byte[] exportWord(UUID id, UserPrincipal principal) {
        Protocol p = find(id, principal);
        String title = "ПРОТОКОЛ № " + (p.getNumber() != null ? p.getNumber() : "")
                + " от " + p.getProtocolDate();

        List<String> paras = new ArrayList<>();
        paras.add("Тип: " + typeName(p.getProtocolType()));
        paras.add("Председатель: " + nvl(p.getChairman()));
        paras.add("Секретарь: " + nvl(p.getSecretary()));
        paras.add("Присутствовали: " + nvl(p.getAttendees()));
        paras.add("");
        paras.add("ПОВЕСТКА ДНЯ:");
        paras.add(nvl(p.getAgenda()));
        paras.add("");
        appendJsonSection(paras, "СЛУШАЛИ:", p.getHeard(), "topic", "speaker", "content");
        appendJsonSection(paras, "РЕШИЛИ:", p.getDecisions(), "decision", "responsible", "deadline");

        byte[] doc = WordExporter.toDocx(title, paras, null, null);
        auditService.record("EXPORT", "protocol", id, null, null, "Экспорт протокола (Word)");
        return doc;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    private void appendJsonSection(List<String> paras, String header, String json, String... fields) {
        paras.add(header);
        if (json != null && !json.isBlank()) {
            try {
                JsonNode arr = objectMapper.readTree(json);
                if (arr.isArray()) {
                    int i = 1;
                    for (JsonNode item : arr) {
                        StringBuilder sb = new StringBuilder(i++ + ". ");
                        for (String f : fields) {
                            if (item.hasNonNull(f)) sb.append(item.get(f).asText()).append("  ");
                        }
                        paras.add(sb.toString().trim());
                    }
                }
            } catch (Exception ignore) {
                paras.add(json);
            }
        }
        paras.add("");
    }

    private Protocol find(UUID id, UserPrincipal principal) {
        return repo.findByIdAndOrganizationId(id, principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Protocol", id));
    }

    private String typeName(String type) {
        return switch (type) {
            case "pedagogical" -> "Педагогический совет";
            case "methodical"  -> "Методический совет";
            case "parents"     -> "Родительское собрание";
            case "guardian"    -> "Попечительский совет";
            case "ethics"      -> "Совет по педагогической этике";
            default            -> type;
        };
    }

    private String nvl(String s) { return s != null ? s : ""; }

    // ── DTO ──────────────────────────────────────────────────────────────────
    @Data
    public static class ProtocolRequest {
        private UUID branchId;
        private String protocolType;
        private String number;
        private LocalDate protocolDate;
        private String language;
        private String chairman, secretary, attendees, agenda;
        private String heard, speakers, decisions; // JSON-массивы
    }
}
