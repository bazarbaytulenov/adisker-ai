package kz.adisker.module.methodistsummary;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Methodist Summary")
@RestController
@RequestMapping("/methodist-summary")
@RequiredArgsConstructor
public class MethodistSummaryController {

    private final MethodistSummaryService service;

    /**
     * Возвращает сводную таблицу наблюдений по группе/периоду.
     * Если данных ещё нет — вычисляет на лету из observation_results.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN','FOUNDER')")
    public ApiResponse<MethodistSummaryService.SummaryResponseDto> getSummary(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam String period,
            @RequestParam String academicYear) {
        return ApiResponse.ok(service.getSummary(organizationId, branchId, groupId, period, academicYear));
    }

    /**
     * Пересчитывает и сохраняет свод в таблицу methodist_summaries.
     */
    @PostMapping("/recalculate")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<MethodistSummaryService.SummaryResponseDto> recalculate(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam(required = false) UUID groupId,
            @RequestParam String period,
            @RequestParam String academicYear) {
        return ApiResponse.ok(service.recalculate(organizationId, branchId, groupId, period, academicYear));
    }
}
