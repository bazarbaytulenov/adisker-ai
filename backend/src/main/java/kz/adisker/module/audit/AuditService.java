package kz.adisker.module.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Централизованная запись событий аудита (ТЗ п.14).
 * Вызывается из бизнес-сервисов и фильтров безопасности.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repo;
    private final ObjectMapper objectMapper;

    /** Полная запись события с данными об объекте и изменениях. */
    public void record(String action, String entityType, UUID entityId,
                        Object oldValue, Object newValue, String notes) {
        try {
            AuditLog.AuditLogBuilder log = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValues(toJson(oldValue))
                    .newValues(toJson(newValue))
                    .notes(notes);

            applyPrincipal(log);
            applyRequestInfo(log);
            repo.save(log.build());
        } catch (Exception e) {
            // Аудит не должен ломать основную операцию
            log.warn("Не удалось записать событие аудита {}/{}: {}", action, entityType, e.getMessage());
        }
    }

    public void record(String action, String entityType, UUID entityId) {
        record(action, entityType, entityId, null, null, null);
    }

    /** Явная запись события входа/выхода с известным пользователем. */
    public void recordAuth(String action, UUID userId, UUID orgId, String email, String notes) {
        try {
            AuditLog.AuditLogBuilder log = AuditLog.builder()
                    .action(action).userId(userId).organizationId(orgId).userEmail(email).notes(notes);
            applyRequestInfo(log);
            repo.save(log.build());
        } catch (Exception e) {
            log.warn("Не удалось записать событие входа {}: {}", action, e.getMessage());
        }
    }

    // ── Чтение журнала ──────────────────────────────────────────────────────────

    public Page<AuditLog> list(UUID organizationId, String action, String entityType, Pageable pageable) {
        if (action != null && !action.isBlank()) {
            return repo.findByOrganizationIdAndActionOrderByCreatedAtDesc(organizationId, action, pageable);
        }
        if (entityType != null && !entityType.isBlank()) {
            return repo.findByOrganizationIdAndEntityTypeOrderByCreatedAtDesc(organizationId, entityType, pageable);
        }
        return repo.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private void applyPrincipal(AuditLog.AuditLogBuilder log) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal p) {
            log.userId(p.getId()).organizationId(p.getOrganizationId()).userEmail(p.getEmail());
        }
    }

    private void applyRequestInfo(AuditLog.AuditLogBuilder log) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            log.ipAddress(clientIp(req)).userAgent(req.getHeader("User-Agent"));
        }
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "\"<unserializable>\"";
        }
    }
}
