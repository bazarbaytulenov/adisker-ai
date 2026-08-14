package kz.adisker.module.schedule;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Schedule")
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService service;

    /** GET /api/schedules?organizationId=&branchId=&groupId=&academicYear=&language= */
    @GetMapping
    public ApiResponse<ScheduleService.ScheduleDto> get(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam UUID groupId,
            @RequestParam String academicYear,
            @RequestParam(defaultValue = "ru") String language) {
        return ApiResponse.ok(service.getOrCreate(organizationId, branchId, groupId, academicYear, language));
    }

    /** PUT /api/schedules/{id}/entries?organizationId= */
    @PutMapping("/{id}/entries")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<ScheduleService.ScheduleDto> saveEntries(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            @RequestBody List<ScheduleService.EntryRequest> entries,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.saveEntries(id, organizationId, entries, principal));
    }

    /** PATCH /api/schedules/{id}/publish?organizationId= */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<ScheduleService.ScheduleDto> publish(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.publish(id, organizationId, principal));
    }
}
