package kz.adisker.module.attendance;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Attendance")
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;

    @GetMapping("/sheet")
    public ApiResponse<AttendanceService.AttendanceSheetDto> getSheet(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam UUID groupId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.ok(service.getOrCreateSheet(organizationId, branchId, groupId, year, month));
    }

    @PatchMapping("/mark/{monthId}")
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<Void> setMark(
            @PathVariable UUID monthId,
            @RequestParam UUID organizationId,
            @RequestBody AttendanceService.MarkRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.setMark(monthId, req.getChildId(), organizationId, req.getDay(), req.getMark(), principal);
        return ApiResponse.ok("Mark saved", null);
    }

    @PatchMapping("/close/{monthId}")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<Void> closeMonth(@PathVariable UUID monthId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        service.closeMonth(monthId, principal);
        return ApiResponse.ok("Month closed", null);
    }
}
