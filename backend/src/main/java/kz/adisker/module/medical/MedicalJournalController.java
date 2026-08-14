package kz.adisker.module.medical;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.common.dto.PageResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Medical Journals")
@RestController
@RequestMapping("/medical")
@RequiredArgsConstructor
public class MedicalJournalController {

    private final MedicalJournalService service;

    @GetMapping
    public ApiResponse<PageResponse<MedicalJournalDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(organizationId, branchId, PageRequest.of(page, size)));
    }

    @GetMapping("/by-type")
    public ApiResponse<List<MedicalJournalDto>> listByType(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam String journalType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.listByType(organizationId, branchId, journalType, from, to));
    }

    @GetMapping("/today")
    public ApiResponse<MedicalJournalDto> getOrCreate(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam String journalType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        return ApiResponse.ok(service.getOrCreate(organizationId, branchId, journalType, d));
    }

    @GetMapping("/{id}")
    public ApiResponse<MedicalJournalDto> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getById(id, principal.getOrganizationId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MedicalJournalDto> create(
            @Valid @RequestBody MedicalJournalRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Journal created", service.create(principal.getOrganizationId(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<MedicalJournalDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalJournalRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, principal.getOrganizationId(), req));
    }

    @PatchMapping("/{id}/data")
    public ApiResponse<MedicalJournalDto> saveData(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String jsonData = body.getOrDefault("data", "{}");
        return ApiResponse.ok(service.saveData(id, principal.getOrganizationId(), jsonData));
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
