package kz.adisker.module.parent;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.module.parent.ParentDtos.*;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Parent Cabinet")
@RestController
@RequiredArgsConstructor
public class ParentController {

    private final ParentService service;

    /**
     * POST /api/auth/register-parent — публичная регистрация родителя по коду приглашения.
     * Путь /auth/** объявлен permitAll в SecurityConfig.
     */
    @PostMapping("/auth/register-parent")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterParentRequest req) {
        var userId = service.registerParent(req);
        return ApiResponse.ok("Регистрация завершена. Войдите под своим email и паролем.",
                Map.of("userId", userId));
    }

    /** GET /api/parent/children — дети текущего родителя. */
    @GetMapping("/parent/children")
    public ApiResponse<List<ChildCardDto>> myChildren(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.myChildren(principal));
    }
}
