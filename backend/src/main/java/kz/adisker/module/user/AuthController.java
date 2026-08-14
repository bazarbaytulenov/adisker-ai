package kz.adisker.module.user;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Auth")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest req,
                                            HttpServletRequest httpRequest) {
        return ApiResponse.ok(authService.login(
                req,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        return ApiResponse.ok(authService.refresh(body.get("refreshToken")));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }
}
