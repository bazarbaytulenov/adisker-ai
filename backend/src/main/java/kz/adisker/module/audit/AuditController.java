package kz.adisker.module.audit;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.common.dto.PageResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Audit")
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService service;

    /** GET /api/audit-logs?action=&entityType=&page=&size= — журнал своей организации. */
    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR','FOUNDER')")
    public ApiResponse<PageResponse<AuditLog>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        var result = service.list(principal.getOrganizationId(), action, entityType,
                PageRequest.of(page, Math.min(size, 200)));
        return ApiResponse.ok(PageResponse.from(result));
    }
}
