package kz.adisker.module.group;

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

import java.util.List;
import java.util.UUID;

@Tag(name = "Groups")
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService service;

    @GetMapping
    public ApiResponse<PageResponse<GroupDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(organizationId, branchId, PageRequest.of(page, size)));
    }

    @GetMapping("/by-branch/{branchId}")
    public ApiResponse<List<GroupDto>> listByBranch(@PathVariable UUID branchId) {
        return ApiResponse.ok(service.listActiveByBranch(branchId));
    }

    @GetMapping("/{id}")
    public ApiResponse<GroupDto> getById(@PathVariable UUID id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getById(id, principal.getOrganizationId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupDto> create(@Valid @RequestBody GroupRequest req,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Group created", service.create(principal.getOrganizationId(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<GroupDto> update(@PathVariable UUID id,
                                        @Valid @RequestBody GroupRequest req,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, principal.getOrganizationId(), req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, principal.getOrganizationId());
        return ApiResponse.ok("Group deleted", null);
    }
}
