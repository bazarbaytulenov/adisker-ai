package kz.adisker.module.janitor;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.janitor.JanitorService.RecordRequest;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Janitor")
@RestController
@RequestMapping("/janitor-records")
@RequiredArgsConstructor
public class JanitorController {

    private final JanitorService service;
    private static final String ROLES = "hasAnyRole('JANITOR','DIRECTOR','SYSTEM_ADMIN')";

    @PostMapping
    @PreAuthorize(ROLES)
    public ApiResponse<JanitorRecord> create(@RequestParam UUID organizationId,
                                             @RequestBody RecordRequest req,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Запись создана", service.create(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ROLES)
    public ApiResponse<JanitorRecord> update(@PathVariable UUID id, @RequestParam UUID organizationId,
                                             @RequestBody RecordRequest req,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, req, principal));
    }

    @GetMapping
    @PreAuthorize(ROLES)
    public ApiResponse<List<JanitorRecord>> list(@RequestParam UUID organizationId,
                                                 @RequestParam(required = false) UUID branchId,
                                                 @RequestParam(required = false) String type,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.list(branchId, type, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ROLES)
    public ApiResponse<JanitorRecord> get(@PathVariable UUID id, @RequestParam UUID organizationId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.get(id, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(ROLES)
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestParam UUID organizationId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, principal);
        return ApiResponse.ok("Удалено", null);
    }
}
