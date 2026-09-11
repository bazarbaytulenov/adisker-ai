package kz.adisker.config;

import kz.adisker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;

/**
 * Аутентификация STOMP-соединения по JWT (ТЗ 5.17, п.10).
 * При CONNECT читает заголовок Authorization: Bearer <token>, валидирует
 * и устанавливает Principal (userId) для WebSocket-сессии.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String bearer = accessor.getFirstNativeHeader("Authorization");
            if (bearer != null && bearer.startsWith("Bearer ")) {
                String token = bearer.substring(7);
                if (jwtService.isTokenValid(token)) {
                    UUID userId = jwtService.extractUserId(token);
                    accessor.setUser(new StompPrincipal(userId.toString()));
                }
            }
        }
        return message;
    }

    /** Простой Principal с идентификатором пользователя. */
    private record StompPrincipal(String name) implements Principal {
        @Override public String getName() { return name; }
    }
}
