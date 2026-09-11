package kz.adisker.module.protocol;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.protocol.ProtocolService.ProtocolRequest;
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

@Tag(name = "Protocols")
@RestController
@RequestMapping("/protocols")
@RequiredArgsConstructor
public class ProtocolController {

    private final ProtocolService service;
    private static final String EDIT = "hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')";
    private static final String VIEW = "hasAnyRole('METHODIST','DIRECTOR','FOUNDER','SYSTEM_ADMIN')";

    @PostMapping
    @PreAuthorize(EDIT)
    public ApiResponse<Protocol> create(@RequestParam UUID organizationId,
                                        @RequestBody ProtocolRequest req,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Протокол создан", service.create(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize(EDIT)
    public ApiResponse<Protocol> update(@PathVariable UUID id, @RequestParam UUID organizationId,
                                        @RequestBody ProtocolRequest req,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.update(id, req, principal));
    }

    @GetMapping
    @PreAuthorize(VIEW)
    public ApiResponse<List<Protocol>> list(@RequestParam UUID organizationId,
                                            @RequestParam(required = false) UUID branchId,
                                            @RequestParam(required = false) String type,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.list(branchId, type, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize(VIEW)
    public ApiResponse<Protocol> get(@PathVariable UUID id, @RequestParam UUID organizationId,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.get(id, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(EDIT)
    public ApiResponse<Void> delete(@PathVariable UUID id, @RequestParam UUID organizationId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, principal);
        return ApiResponse.ok("Удалено", null);
    }

    /** GET /api/protocols/{id}/export/word — печать протокола в DOCX. */
    @GetMapping("/{id}/export/word")
    @PreAuthorize(VIEW)
    public ResponseEntity<byte[]> exportWord(@PathVariable UUID id, @RequestParam UUID organizationId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        byte[] body = service.exportWord(id, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"protocol-" + id + ".docx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(body);
    }
}
