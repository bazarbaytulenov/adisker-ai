package kz.adisker.module.chat;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.common.dto.PageResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Chat")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;

    // ── REST endpoints ────────────────────────────────────────────────────────

    /** Get or create a chat thread between educator and parent for a child */
    @GetMapping("/thread")
    public ApiResponse<ChatDto.ThreadDto> getOrCreateThread(
            @RequestParam UUID childId,
            @RequestParam UUID educatorId,
            @RequestParam(required = false) UUID parentUserId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.getOrCreateThread(
                principal.getOrganizationId(), childId, educatorId, parentUserId));
    }

    /** List all threads for the current user */
    @GetMapping("/threads")
    public ApiResponse<List<ChatDto.ThreadDto>> listThreads(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(service.listThreadsForUser(
                principal.getOrganizationId(),
                principal.getId(),
                principal.getRoleCode()));
    }

    /** List messages in a thread (paginated, newest first) */
    @GetMapping("/threads/{threadId}/messages")
    public ApiResponse<PageResponse<ChatDto.MessageDto>> listMessages(
            @PathVariable UUID threadId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.ok(service.listMessages(threadId, page, size));
    }

    /** Send a message via REST (fallback for non-WebSocket clients) */
    @PostMapping("/threads/{threadId}/messages")
    public ApiResponse<ChatDto.MessageDto> sendMessage(
            @PathVariable UUID threadId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String content = body.getOrDefault("content", "").trim();
        return ApiResponse.ok(service.sendMessage(
                principal.getOrganizationId(), threadId, principal.getId(), content));
    }

    /** Mark all messages in a thread as read */
    @PatchMapping("/threads/{threadId}/read")
    public ApiResponse<Void> markRead(
            @PathVariable UUID threadId,
            @AuthenticationPrincipal UserPrincipal principal) {
        service.markRead(threadId, principal.getId());
        return ApiResponse.ok("Marked as read", null);
    }

    // ── WebSocket STOMP endpoints ─────────────────────────────────────────────

    /**
     * Client sends to: /app/chat.send
     * Payload: { "threadId": "...", "content": "..." }
     * Server broadcasts to: /topic/chat/{threadId}
     */
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload ChatDto.SendMessagePayload payload,
                              Principal principal) {
        // principal.getName() == user email (from JWT/STOMP auth)
        // For simplicity we rely on REST send; WebSocket just routes the broadcast
        // Full JWT-in-STOMP auth can be added in ChannelInterceptor
    }
}
