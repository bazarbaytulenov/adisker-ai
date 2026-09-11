package kz.adisker.module.manager;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.manager.ManagerOrderService.OrderRequest;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Manager Orders")
@RestController
@RequestMapping("/manager-orders")
@RequiredArgsConstructor
public class ManagerOrderController {

    private final ManagerOrderService service;
    private static final String ROLES = "hasAnyRole('DIRECTOR','SYSTEM_ADMIN')";
    private static final String VIEW  = "hasAnyRole('DIRECTOR','FOUNDER','SYSTEM_ADMIN')";

    @PostMapping
    @PreAuthorize(ROLES)
    public ApiResponse<ManagerOrder> create(@RequestParam UUID organizationId,
                                            @RequestBody OrderRequest req,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Приказ создан", service.create(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize(ROLES)
    public ApiResponse<ManagerOrder> update(@PathVariable UUID id, @RequestParam UUID organizationId,
                                            @RequestBody OrderRequest req,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, req, principal));
    }

    @PatchMapping("/{id}/sign")
    @PreAuthorize(ROLES)
    public ApiResponse<ManagerOrder> sign(@PathVariable UUID id, @RequestParam UUID organizationId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Приказ подписан", service.sign(id, principal));
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public ApiResponse<List<ManagerOrder>> list(@RequestParam UUID organizationId,
                                                @RequestParam(required = false) UUID branchId,
                                                @RequestParam(required = false) String type,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.list(branchId, type, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public ApiResponse<ManagerOrder> get(@PathVariable UUID id, @RequestParam UUID organizationId,
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

    @GetMapping("/{id}/export/word")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportWord(@PathVariable UUID id, @RequestParam UUID organizationId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        byte[] body = service.exportWord(id, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"order-" + id + ".docx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(body);
    }
}
