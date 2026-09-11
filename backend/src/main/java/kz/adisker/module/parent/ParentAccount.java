package kz.adisker.module.parent;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Связь родителя (user с ролью PARENT) с ребёнком (ТЗ 5.20, 5.21).
 * Родитель может быть привязан к нескольким детям (по одному приглашению на ребёнка),
 * но каждое приглашение привязывает только к одному ребёнку.
 */
@Entity
@Table(name = "parent_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParentAccount {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @Column(name = "user_id",         nullable = false) private UUID userId;
    @Column(name = "child_id",        nullable = false) private UUID childId;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "invitation_id")   private UUID invitationId;

    @Column(name = "consent_given", nullable = false) private boolean consentGiven = false;
    @Column(name = "consent_date")  private Instant consentDate;
    @Column(name = "is_active",     nullable = false) private boolean active = true;
    @Column(name = "created_at",    nullable = false) private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
