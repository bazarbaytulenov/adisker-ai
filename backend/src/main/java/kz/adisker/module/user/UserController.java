package kz.adisker.module.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.common.dto.PageResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Users")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getMe(principal));
    }

    /** Смена собственного пароля (любой авторизованный пользователь). */
    @PatchMapping("/me/password")
    public ApiResponse<Void> changeMyPassword(@Valid @RequestBody ChangePasswordRequest req,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        userService.changeOwnPassword(principal, req.currentPassword(), req.newPassword());
        return ApiResponse.ok("Password changed", null);
    }

    public record ChangePasswordRequest(
            @jakarta.validation.constraints.NotBlank String currentPassword,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(min = 6) String newPassword) {}

    @GetMapping("/assignable-roles")
    public ApiResponse<java.util.List<String>> assignableRoles(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.assignableRoles(principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR','METHODIST')")
    public ApiResponse<PageResponse<UserDto>> list(
            @RequestParam UUID organizationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(userService.getByOrg(organizationId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserDto> getById(@PathVariable UUID id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.getById(id, principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ApiResponse<UserDto> create(@Valid @RequestBody UserRequest req,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("User created", userService.create(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ApiResponse<UserDto> update(@PathVariable UUID id,
                                       @Valid @RequestBody UserRequest req,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(userService.update(id, req, principal));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ApiResponse<Void> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ApiResponse.ok("User deactivated", null);
    }

    /** Сброс пароля пользователю (супер-админ — любому; директор — своим сотрудникам). */
    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','DIRECTOR')")
    public ApiResponse<Void> resetPassword(@PathVariable UUID id,
                                           @Valid @RequestBody ResetPasswordRequest req,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        userService.resetPassword(id, req.password(), principal);
        return ApiResponse.ok("Password reset", null);
    }

    /** Админы (директора) организации — для супер-админа. */
    @GetMapping("/org-admins")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ApiResponse<java.util.List<UserDto>> orgAdmins(@RequestParam UUID organizationId) {
        return ApiResponse.ok(userService.getOrgAdmins(organizationId));
    }

    public record ResetPasswordRequest(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(min = 6) String password) {}
}
