package kz.adisker.module.chat;

import kz.adisker.common.RoleCode;
import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatThreadRepository threadRepo;
    private final ChatMessageRepository messageRepo;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Threads ───────────────────────────────────────────────────────────────

    @Transactional
    public ChatDto.ThreadDto getOrCreateThread(UUID orgId, UUID childId, UUID educatorId, UUID parentUserId) {
        ChatThread thread = threadRepo.findByChildIdAndEducatorId(childId, educatorId)
                .orElseGet(() -> threadRepo.save(ChatThread.builder()
                        .organizationId(orgId)
                        .childId(childId)
                        .educatorId(educatorId)
                        .parentUserId(parentUserId)
                        .active(true)
                        .build()));
        return toThreadDto(thread);
    }

    public List<ChatDto.ThreadDto> listThreadsForUser(UUID orgId, UUID userId, String role) {
        List<ChatThread> threads = RoleCode.PARENT.equals(role)
                ? threadRepo.findByOrganizationIdAndParentUserIdAndActiveTrue(orgId, userId)
                : threadRepo.findByOrganizationIdAndEducatorIdAndActiveTrue(orgId, userId);

        return threads.stream().map(t -> {
            long unread = messageRepo.countByThreadIdAndReadFalseAndSenderIdNot(t.getId(), userId);
            ChatDto.MessageDto last = messageRepo
                    .findByThreadIdOrderByCreatedAtDesc(t.getId(), PageRequest.of(0, 1))
                    .stream().findFirst()
                    .map(this::toMessageDto)
                    .orElse(null);
            ChatDto.ThreadDto dto = toThreadDto(t);
            dto.setUnreadCount(unread);
            dto.setLastMessage(last);
            return dto;
        }).collect(Collectors.toList());
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    public PageResponse<ChatDto.MessageDto> listMessages(UUID threadId, int page, int size) {
        return PageResponse.from(
                messageRepo.findByThreadIdOrderByCreatedAtDesc(threadId, PageRequest.of(page, size))
                        .map(this::toMessageDto)
        );
    }

    @Transactional
    public ChatDto.MessageDto sendMessage(UUID orgId, UUID threadId, UUID senderId, String content) {
        ChatThread thread = threadRepo.findByIdAndOrganizationId(threadId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatThread", threadId));

        ChatMessage msg = messageRepo.save(ChatMessage.builder()
                .threadId(thread.getId())
                .senderId(senderId)
                .organizationId(orgId)
                .content(content)
                .read(false)
                .build());

        ChatDto.MessageDto dto = toMessageDto(msg);

        // Push real-time event to all subscribers of this thread
        ChatDto.MessageEvent event = new ChatDto.MessageEvent();
        event.setThreadId(threadId);
        event.setMessage(dto);
        messagingTemplate.convertAndSend("/topic/chat/" + threadId, event);

        return dto;
    }

    @Transactional
    public void markRead(UUID threadId, UUID readerId) {
        messageRepo.markAllReadInThread(threadId, readerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ChatDto.ThreadDto toThreadDto(ChatThread t) {
        ChatDto.ThreadDto dto = new ChatDto.ThreadDto();
        dto.setId(t.getId());
        dto.setOrganizationId(t.getOrganizationId());
        dto.setChildId(t.getChildId());
        dto.setEducatorId(t.getEducatorId());
        dto.setParentUserId(t.getParentUserId());
        dto.setActive(t.isActive());
        dto.setCreatedAt(t.getCreatedAt());
        return dto;
    }

    private ChatDto.MessageDto toMessageDto(ChatMessage m) {
        ChatDto.MessageDto dto = new ChatDto.MessageDto();
        dto.setId(m.getId());
        dto.setThreadId(m.getThreadId());
        dto.setSenderId(m.getSenderId());
        dto.setContent(m.getContent());
        dto.setRead(m.isRead());
        dto.setReadAt(m.getReadAt());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }
}
