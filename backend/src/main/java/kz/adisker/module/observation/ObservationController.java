package kz.adisker.module.observation;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Tag(name = "Observation & IKR")
@RestController
@RequestMapping("/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService service;

    @GetMapping
    public ApiResponse<ObservationService.ObservationDto> get(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam UUID groupId,
            @RequestParam UUID childId,
            @RequestParam String period,
            @RequestParam String academicYear,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getOrCreate(
                organizationId, branchId, groupId, childId, period, academicYear, principal.getId()));
    }

    @PatchMapping("/{id}/result")
    public ApiResponse<Void> setResult(
            @PathVariable UUID id,
            @RequestBody ObservationService.SetResultRequest req) {
        service.setResult(id, req.getIndicatorId(), req.getLevel());
        return ApiResponse.ok("Result saved", null);
    }

    @GetMapping("/card")
    public ApiResponse<ObservationService.IndividualCardDto> getCard(
            @RequestParam UUID childId,
            @RequestParam UUID observationId) {
        return ApiResponse.ok(service.getCard(childId, observationId));
    }

    @PostMapping("/card")
    public ApiResponse<ObservationService.IndividualCardDto> saveCard(
            @RequestParam UUID childId,
            @RequestParam UUID observationId,
            @RequestBody ObservationService.SaveCardRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.saveCard(
                principal.getOrganizationId(), childId, observationId,
                req.getGameName(), req.getGameObjectives(),
                req.getGameProcedure(), req.getCustomNotes(), req.getLanguage()));
    }
}
