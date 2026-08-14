package kz.adisker.module.branch;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Branches")
@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','FOUNDER','DIRECTOR','METHODIST')")
    public ApiResponse<PageResponse<BranchDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.getByOrg(organizationId, PageRequest.of(page, size)));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','FOUNDER','DIRECTOR','METHODIST')")
    public ApiResponse<List<BranchDto>> listActive(@RequestParam UUID organizationId) {
        return ApiResponse.ok(service.getAllActiveByOrg(organizationId));
    }

    @GetMapping("/{id}")
    public ApiResponse<BranchDto> getById(@PathVariable UUID id, @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.getById(id, organizationId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ApiResponse<BranchDto> create(@RequestParam UUID organizationId,
                                         @Valid @RequestBody BranchRequest req) {
        return ApiResponse.ok("Branch created", service.create(organizationId, req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ApiResponse<BranchDto> update(@PathVariable UUID id,
                                         @RequestParam UUID organizationId,
                                         @Valid @RequestBody BranchRequest req) {
        return ApiResponse.ok(service.update(id, organizationId, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestParam UUID organizationId) {
        service.delete(id, organizationId);
        return ApiResponse.ok("Branch deleted", null);
    }
}
