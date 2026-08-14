package kz.adisker.module.library;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.common.dto.PageResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Library Fund")
@RestController
@RequestMapping("/library")
@RequiredArgsConstructor
public class LibraryFundController {

    private final LibraryFundService service;

    @GetMapping
    public ApiResponse<PageResponse<LibraryFundDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(organizationId, branchId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<LibraryFundDto> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getById(id, principal.getOrganizationId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LibraryFundDto> create(
            @Valid @RequestBody LibraryFundRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Library item created", service.create(principal.getOrganizationId(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<LibraryFundDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody LibraryFundRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, principal.getOrganizationId(), req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, principal.getOrganizationId());
        return ApiResponse.ok("Deleted", null);
    }
}
