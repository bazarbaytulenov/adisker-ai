package kz.adisker.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kz.adisker.common.RoleCode;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Сквозная защита многоарендности (ТЗ 4.2, приёмочный сценарий A-10).
 * <p>
 * Любой запрос, содержащий параметр {@code organizationId}, проверяется на
 * соответствие организации текущего пользователя (из JWT). При попытке обратиться
 * к чужой организации возвращается 403 — независимо от того, использует ли
 * конкретный сервис этот параметр для фильтрации.
 * <p>
 * SYSTEM_ADMIN работает со всеми организациями и от проверки освобождён.
 */
@Component
public class TenantGuardInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {

        String requested = request.getParameter("organizationId");
        if (requested == null || requested.isBlank()) {
            return true; // нет параметра — нечего сверять
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return true; // неаутентифицированные запросы отсекает Spring Security
        }

        // Системный администратор — кросс-организационный доступ
        if (RoleCode.SYSTEM_ADMIN.equals(principal.getRoleCode())) {
            return true;
        }

        UUID requestedOrg;
        try {
            requestedOrg = UUID.fromString(requested.trim());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Некорректный organizationId");
            return false;
        }

        if (!requestedOrg.equals(principal.getOrganizationId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Доступ к данным другой организации запрещён");
            return false;
        }

        return true;
    }
}
