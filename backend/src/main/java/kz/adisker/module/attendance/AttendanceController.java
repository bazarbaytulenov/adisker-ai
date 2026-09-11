package kz.adisker.module.attendance;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.export.ExcelExporter;
import kz.adisker.module.export.PdfTableExporter;
import kz.adisker.module.audit.AuditService;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Attendance")
@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService service;
    private final AuditService auditService;

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

    /** GET /api/attendance/export/xlsx — табель в Excel. */
    @GetMapping("/export/xlsx")
    public ResponseEntity<byte[]> exportXlsx(
            @RequestParam UUID organizationId, @RequestParam UUID branchId,
            @RequestParam UUID groupId, @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserPrincipal principal) {
        var data = service.exportData(organizationId, branchId, groupId, year, month);
        String title = "Табель посещаемости " + month + "/" + year;
        byte[] body = ExcelExporter.toXlsx(title, data.headers(), toObjectRows(data.rows()));
        auditService.record("EXPORT", "attendance", groupId, null, null, title + " (XLSX)");
        return fileResponse(body, "tabel-" + year + "-" + month + ".xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    /** GET /api/attendance/export/pdf — табель в PDF. */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam UUID organizationId, @RequestParam UUID branchId,
            @RequestParam UUID groupId, @RequestParam int year, @RequestParam int month,
            @AuthenticationPrincipal UserPrincipal principal) {
        var data = service.exportData(organizationId, branchId, groupId, year, month);
        String title = "Табель посещаемости " + month + "/" + year;
        byte[] body = PdfTableExporter.toPdf(title, data.headers(), data.rows(), principal.getEmail());
        auditService.record("EXPORT", "attendance", groupId, null, null, title + " (PDF)");
        return fileResponse(body, "tabel-" + year + "-" + month + ".pdf", MediaType.APPLICATION_PDF_VALUE);
    }

    private static List<List<Object>> toObjectRows(List<List<String>> rows) {
        return rows.stream().map(r -> r.stream().map(o -> (Object) o).toList()).toList();
    }

    private static ResponseEntity<byte[]> fileResponse(byte[] body, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
