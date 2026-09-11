package kz.adisker.module.nomenclature;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.nomenclature.NomenclatureService.Request;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Nomenclature")
@RestController
@RequestMapping("/nomenclature")
@RequiredArgsConstructor
public class NomenclatureController {

    private final NomenclatureService service;
    private static final String EDIT = "hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')";
    private static final String VIEW = "hasAnyRole('METHODIST','DIRECTOR','FOUNDER','SYSTEM_ADMIN')";

    @PostMapping
    @PreAuthorize(EDIT)
    public ApiResponse<Nomenclature> create(@RequestParam UUID organizationId, @RequestBody Request req,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Создано", service.create(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize(EDIT)
    public ApiResponse<Nomenclature> update(@PathVariable UUID id, @RequestParam UUID organizationId,
                                            @RequestBody Request req, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, req, principal));
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public ApiResponse<List<Nomenclature>> list(@RequestParam UUID organizationId,
                                                @RequestParam(required = false) String search,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.list(search, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(EDIT)
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestParam UUID organizationId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, principal);
        return ApiResponse.ok("Удалено", null);
    }
}
