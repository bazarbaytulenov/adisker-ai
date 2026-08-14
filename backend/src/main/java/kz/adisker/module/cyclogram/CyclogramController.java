package kz.adisker.module.cyclogram;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Cyclogram")
@RestController
@RequestMapping("/cyclograms")
@RequiredArgsConstructor
public class CyclogramController {

    private final CyclogramService service;

    /**
     * GET /api/cyclograms?organizationId=...&branchId=...&groupId=...
     * Список циклограмм. branchId и groupId — опциональные фильтры.
     */
    @GetMapping
    public ApiResponse<List<CyclogramService.CyclogramDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID groupId) {
        return ApiResponse.ok(service.list(organizationId, branchId, groupId));
    }

    /**
     * GET /api/cyclograms/{id}?organizationId=...
     * Получить циклограмму по ID.
     */
    @GetMapping("/{id}")
    public ApiResponse<CyclogramService.CyclogramDto> get(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.get(id, organizationId));
    }

    /**
     * POST /api/cyclograms
     * Создать новую циклограмму.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<CyclogramService.CyclogramDto> create(
            @RequestBody CyclogramRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Циклограмма создана", service.create(req, principal));
    }

    /**
     * PUT /api/cyclograms/{id}
     * Обновить содержимое циклограммы.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<CyclogramService.CyclogramDto> update(
            @PathVariable UUID id,
            @RequestBody CyclogramRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Циклограмма обновлена", service.update(id, req, principal));
    }

    /**
     * PATCH /api/cyclograms/{id}/approve?organizationId=...
     * Утвердить циклограмму (только методист/директор).
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<CyclogramService.CyclogramDto> approve(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        return ApiResponse.ok("Циклограмма утверждена", service.approve(id, organizationId));
    }

    /**
     * DELETE /api/cyclograms/{id}?organizationId=...
     * Удалить циклограмму (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        service.delete(id, organizationId);
        return ApiResponse.ok("Циклограмма удалена", null);
    }
}
