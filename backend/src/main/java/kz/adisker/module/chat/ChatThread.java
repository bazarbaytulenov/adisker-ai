package kz.adisker.module.chat;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "chat_threads",
    uniqueConstraints = @UniqueConstraint(columnNames = {"child_id", "educator_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "child_id", nullable = false)
    private UUID childId;

    @Column(name = "educator_id", nullable = false)
    private UUID educatorId;

    @Column(name = "parent_user_id")
    private UUID parentUserId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;
}
