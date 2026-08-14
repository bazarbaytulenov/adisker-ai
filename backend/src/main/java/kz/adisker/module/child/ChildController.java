package kz.adisker.module.child;

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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Children")
@RestController
@RequestMapping("/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildService service;

    @GetMapping
    public ApiResponse<PageResponse<ChildDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(organizationId, branchId, groupId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ChildDto> getById(@PathVariable UUID id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getById(id, principal.getOrganizationId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChildDto> create(@Valid @RequestBody ChildRequest req,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Child created", service.create(principal.getOrganizationId(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ChildDto> update(@PathVariable UUID id,
                                        @Valid @RequestBody ChildRequest req,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, principal.getOrganizationId(), req));
    }

    @PatchMapping("/{id}/discharge")
    public ApiResponse<Void> discharge(@PathVariable UUID id,
                                       @RequestBody Map<String, String> body,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        service.discharge(
                id,
                principal.getOrganizationId(),
                LocalDate.parse(body.get("dischargeDate")),
                body.get("reason"),
                body.get("orderNum"));
        return ApiResponse.ok("Child discharged", null);
    }
}
