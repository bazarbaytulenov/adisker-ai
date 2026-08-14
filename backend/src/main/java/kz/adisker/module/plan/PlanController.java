package kz.adisker.module.plan;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Plan")
@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService service;

    /** GET /api/plans?organizationId=&branchId=&groupId=&year=&month=&week=&language= */
    @GetMapping
    public ApiResponse<PlanService.PlanDto> getOrCreate(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam UUID groupId,
            @RequestParam String year,
            @RequestParam int month,
            @RequestParam int week,
            @RequestParam(defaultValue = "ru") String language,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getOrCreate(organizationId, branchId, groupId, year, month, week, language, principal));
    }

    /** GET /api/plans/list?organizationId=&groupId=&year=&language= */
    @GetMapping("/list")
    public ApiResponse<List<PlanService.PlanDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam UUID groupId,
            @RequestParam String year,
            @RequestParam(defaultValue = "ru") String language) {
        return ApiResponse.ok(service.listByGroup(organizationId, groupId, year, language));
    }

    /** GET /api/plans/{planId}/sections */
    @GetMapping("/{planId}/sections")
    public ApiResponse<List<PlanService.SectionDto>> getSections(@PathVariable UUID planId) {
        return ApiResponse.ok(service.getSections(planId));
    }

    /** PUT /api/plans/{planId}/sections?organizationId= */
    @PutMapping("/{planId}/sections")
    @PreAuthorize("hasAnyRole('EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<PlanService.SectionDto> saveSection(
            @PathVariable UUID planId,
            @RequestParam UUID organizationId,
            @RequestBody SectionRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.saveSection(planId, organizationId, req, principal));
    }

    /** PATCH /api/plans/sections/{sectionId}/submit */
    @PatchMapping("/sections/{sectionId}/submit")
    @PreAuthorize("hasAnyRole('EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<PlanService.SectionDto> submit(
            @PathVariable UUID sectionId,
            @RequestParam UUID organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.submitSection(sectionId, organizationId, principal));
    }

    /** PATCH /api/plans/sections/{sectionId}/approve */
    @PatchMapping("/sections/{sectionId}/approve")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<PlanService.SectionDto> approve(
            @PathVariable UUID sectionId,
            @RequestParam UUID organizationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.approveSection(sectionId, organizationId, principal));
    }

    /** PATCH /api/plans/sections/{sectionId}/return */
    @PatchMapping("/sections/{sectionId}/return")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<PlanService.SectionDto> returnSection(
            @PathVariable UUID sectionId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.returnSection(sectionId, body.get("comment"), principal));
    }

    /** POST /api/plans/sections/{sectionId}/lock */
    @PostMapping("/sections/{sectionId}/lock")
    @PreAuthorize("hasAnyRole('EDUCATOR','KAZ_TEACHER','MUSIC_TEACHER','PE_INSTRUCTOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<Void> lock(
            @PathVariable UUID sectionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.lockSection(sectionId, principal);
        return ApiResponse.ok("Заблокировано", null);
    }

    /** DELETE /api/plans/sections/{sectionId}/lock */
    @DeleteMapping("/sections/{sectionId}/lock")
    public ApiResponse<Void> unlock(
            @PathVariable UUID sectionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.unlockSection(sectionId, principal);
        return ApiResponse.ok("Разблокировано", null);
    }
}
