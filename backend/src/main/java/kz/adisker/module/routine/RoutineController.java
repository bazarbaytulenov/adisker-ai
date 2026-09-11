package kz.adisker.module.routine;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.routine.RoutineService.RoutineRequest;
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

@Tag(name = "Routine")
@RestController
@RequestMapping("/routines")
@RequiredArgsConstructor
public class RoutineController {

    private final RoutineService service;
    private static final String EDIT = "hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')";

    /** GET /api/routines?branchId=&groupId=&year=&language= — получить/создать. */
    @GetMapping
    public ApiResponse<Routine> getOrCreate(
            @RequestParam UUID organizationId, @RequestParam UUID branchId,
            @RequestParam UUID groupId, @RequestParam String year,
            @RequestParam(defaultValue = "ru") String language,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getOrCreate(branchId, groupId, year, language, principal));
    }

    @GetMapping("/by-group")
    public ApiResponse<List<Routine>> byGroup(@RequestParam UUID organizationId,
                                              @RequestParam UUID groupId) {
        return ApiResponse.ok(service.listByGroup(groupId));
    }

    @PutMapping("/{id}")
    @PreAuthorize(EDIT)
    public ApiResponse<Routine> save(@PathVariable UUID id, @RequestParam UUID organizationId,
                                     @RequestBody RoutineRequest req,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.save(id, req, principal));
    }

    @PatchMapping("/{id}/template")
    @PreAuthorize(EDIT)
    public ApiResponse<Routine> applyTemplate(@PathVariable UUID id, @RequestParam UUID organizationId,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Заполнено по шаблону", service.applyTemplate(id, principal));
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize(EDIT)
    public ApiResponse<Routine> publish(@PathVariable UUID id, @RequestParam UUID organizationId,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Опубликовано", service.publish(id, principal));
    }

    @PatchMapping("/{id}/unpublish")
    @PreAuthorize(EDIT)
    public ApiResponse<Routine> unpublish(@PathVariable UUID id, @RequestParam UUID organizationId,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.unpublish(id, principal));
    }

    @GetMapping("/{id}/export/word")
    public ResponseEntity<byte[]> exportWord(@PathVariable UUID id, @RequestParam UUID organizationId,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        byte[] body = service.exportWord(id, principal);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"routine-" + id + ".docx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(body);
    }
}
