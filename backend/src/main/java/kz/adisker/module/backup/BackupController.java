package kz.adisker.module.backup;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Backup")
@RestController
@RequestMapping("/backup")
@RequiredArgsConstructor
public class BackupController {

    private final BackupService service;

    /** GET /api/backup/export?organizationId= — экспорт организации в JSON-файл. */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ResponseEntity<byte[]> export(@RequestParam UUID organizationId,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        String json = service.exportOrganization(organizationId, principal);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"backup-" + organizationId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /** POST /api/backup/restore-log?organizationId= — зафиксировать восстановление. */
    @PostMapping("/restore-log")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ApiResponse<Void> restoreLog(@RequestParam UUID organizationId,
                                        @RequestBody(required = false) Map<String, String> body,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        service.registerRestore(organizationId, body != null ? body.get("note") : null, principal);
        return ApiResponse.ok("Восстановление зафиксировано в журнале", null);
    }
}
