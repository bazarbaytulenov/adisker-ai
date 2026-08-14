package kz.adisker.module.chat;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

public class ChatDto {

    @Data
    public static class ThreadDto {
        private UUID id;
        private UUID organizationId;
        private UUID childId;
        private String childName;       // populated from join
        private UUID educatorId;
        private String educatorName;    // populated from join
        private UUID parentUserId;
        private boolean active;
        private Instant createdAt;
        private long unreadCount;
        private MessageDto lastMessage;
    }

    @Data
    public static class MessageDto {
        private UUID id;
        private UUID threadId;
        private UUID senderId;
        private String senderName;      // populated from join
        private String content;
        private boolean read;
        private Instant readAt;
        private Instant createdAt;
    }

    /** Payload sent via WebSocket STOMP from client */
    @Data
    public static class SendMessagePayload {
        private UUID threadId;
        private String content;
    }

    /** Event pushed to subscribers via STOMP broker */
    @Data
    public static class MessageEvent {
        private UUID threadId;
        private MessageDto message;
    }
}
