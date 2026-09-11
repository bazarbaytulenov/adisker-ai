package kz.adisker.module.ai;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "AI")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService service;
    private static final String ROLES = "hasAnyRole('METHODIST','DIRECTOR','EDUCATOR','SYSTEM_ADMIN')";

    /** GET /api/ai/status — доступна ли AI-интеграция. */
    @GetMapping("/status")
    public ApiResponse<Map<String, Boolean>> status() {
        return ApiResponse.ok(Map.of("available", service.available()));
    }

    /** POST /api/ai/cyclogram — сгенерировать черновик циклограммы. */
    @PostMapping("/cyclogram")
    @PreAuthorize(ROLES)
    public ApiResponse<Map<String, String>> cyclogram(@RequestBody Map<String, String> body,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        String text = service.generateCyclogram(
                body.getOrDefault("ageGroup", ""), body.getOrDefault("theme", ""),
                body.getOrDefault("language", "ru"), principal);
        return ApiResponse.ok(Map.of("draft", text));
    }

    /** POST /api/ai/recommendations — рекомендации для родителей. */
    @PostMapping("/recommendations")
    @PreAuthorize(ROLES)
    public ApiResponse<Map<String, String>> recommendations(@RequestBody Map<String, String> body,
                                                            @AuthenticationPrincipal UserPrincipal principal) {
        String text = service.generateRecommendations(
                body.getOrDefault("childAge", ""), body.getOrDefault("summary", ""),
                body.getOrDefault("language", "ru"), principal);
        return ApiResponse.ok(Map.of("draft", text));
    }

    /** POST /api/ai/text — произвольная генерация. */
    @PostMapping("/text")
    @PreAuthorize(ROLES)
    public ApiResponse<Map<String, String>> text(@RequestBody Map<String, String> body,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(Map.of("draft", service.generateText(body.getOrDefault("prompt", ""), principal)));
    }
}
