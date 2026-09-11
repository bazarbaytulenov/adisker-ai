package kz.adisker.module.annual;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.annual.AnnualPlanService.*;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Annual & Monthly Plans")
@RestController
@RequestMapping("/annual-plans")
@RequiredArgsConstructor
public class AnnualPlanController {

    private final AnnualPlanService service;

    private static final String EDIT_ROLES = "hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')";
    private static final String VIEW_ROLES = "hasAnyRole('METHODIST','DIRECTOR','FOUNDER','SYSTEM_ADMIN')";

    /** GET /api/annual-plans?branchId=&year=&language= — получить/создать годовой план. */
    @GetMapping
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<AnnualPlanDto> getOrCreate(
            @RequestParam UUID organizationId, @RequestParam UUID branchId,
            @RequestParam String year, @RequestParam(defaultValue = "ru") String language,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getOrCreatePlan(branchId, year, language, principal));
    }

    /** GET /api/annual-plans/list?branchId= */
    @GetMapping("/list")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<AnnualPlanDto>> list(
            @RequestParam UUID organizationId, @RequestParam UUID branchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.listPlans(branchId, principal));
    }

    @PostMapping("/{planId}/sections")
    @PreAuthorize(EDIT_ROLES)
    public ApiResponse<SectionDto> addSection(
            @PathVariable UUID planId, @RequestParam UUID organizationId,
            @RequestBody Map<String, Object> body, @AuthenticationPrincipal UserPrincipal principal) {
        int sort = body.get("sortOrder") != null ? ((Number) body.get("sortOrder")).intValue() : 0;
        return ApiResponse.ok(service.addSection(planId, (String) body.get("title"), sort, principal));
    }

    @GetMapping("/{planId}/sections")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<SectionDto>> sections(@PathVariable UUID planId,
                                                  @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.listSections(planId));
    }

    @PostMapping("/sections/{sectionId}/events")
    @PreAuthorize(EDIT_ROLES)
    public ApiResponse<EventDto> addEvent(
            @PathVariable UUID sectionId, @RequestParam UUID organizationId,
            @RequestBody EventRequest req, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.addEvent(sectionId, req, principal));
    }

    @GetMapping("/sections/{sectionId}/events")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<EventDto>> events(@PathVariable UUID sectionId,
                                              @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.listEvents(sectionId));
    }

    @PatchMapping("/{planId}/approve")
    @PreAuthorize(EDIT_ROLES)
    public ApiResponse<AnnualPlanDto> approve(@PathVariable UUID planId,
                                              @RequestParam UUID organizationId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Утверждён", service.approvePlan(planId, principal));
    }

    /** POST /api/annual-plans/{planId}/propagate?year=&month= — авто-передача в месячный план. */
    @PostMapping("/{planId}/propagate")
    @PreAuthorize(EDIT_ROLES)
    public ApiResponse<Map<String, Integer>> propagate(
            @PathVariable UUID planId, @RequestParam UUID organizationId,
            @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserPrincipal principal) {
        int n = service.propagateToMonthly(planId, year, month, principal);
        return ApiResponse.ok("Перенесено мероприятий: " + n, Map.of("propagated", n));
    }

    // ── Месячные планы ──────────────────────────────────────────────────────────

    @GetMapping("/monthly")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<MonthlyPlanDto>> monthlyPlans(
            @RequestParam UUID organizationId, @RequestParam UUID branchId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.listMonthlyPlans(branchId, principal));
    }

    @GetMapping("/monthly/{monthlyPlanId}/events")
    @PreAuthorize(VIEW_ROLES)
    public ApiResponse<List<MonthlyEventDto>> monthlyEvents(@PathVariable UUID monthlyPlanId,
                                                            @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.listMonthlyEvents(monthlyPlanId));
    }
}
