package kz.adisker.module.parent;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.parent.ParentDtos.*;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Parent Invitations")
@RestController
@RequestMapping("/parent-invitations")
@RequiredArgsConstructor
public class ParentInvitationController {

    private final ParentService service;

    /** POST /api/parent-invitations — создать приглашение (ссылка/QR/WhatsApp). */
    @PostMapping
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<InvitationDto> create(
            @Valid @RequestBody CreateInvitationRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Приглашение создано", service.createInvitation(req, principal));
    }

    /** GET /api/parent-invitations — список приглашений организации. */
    @GetMapping
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<List<InvitationDto>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.listInvitations(principal));
    }

    /** PATCH /api/parent-invitations/{id}/revoke — отозвать приглашение. */
    @PatchMapping("/{id}/revoke")
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<Void> revoke(@PathVariable UUID id,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        service.revokeInvitation(id, principal);
        return ApiResponse.ok("Приглашение отозвано", null);
    }
}
