package kz.adisker.module.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByThreadIdOrderByCreatedAtDesc(UUID threadId, Pageable pageable);

    List<ChatMessage> findByThreadIdAndReadFalseAndSenderIdNot(UUID threadId, UUID myId);

    long countByThreadIdAndReadFalseAndSenderIdNot(UUID threadId, UUID myId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.read = true, m.readAt = CURRENT_TIMESTAMP WHERE m.threadId = :threadId AND m.senderId <> :myId AND m.read = false")
    void markAllReadInThread(@Param("threadId") UUID threadId, @Param("myId") UUID myId);
}
