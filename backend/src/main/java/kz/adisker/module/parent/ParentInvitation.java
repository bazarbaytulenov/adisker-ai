package kz.adisker.module.parent;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Приглашение родителя (ТЗ 5.21). Одноразовый код/ссылка/QR для регистрации,
 * привязанный к конкретному ребёнку. Статусы: active / used / revoked.
 */
@Entity
@Table(name = "parent_invitations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParentInvitation {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "child_id",        nullable = false) private UUID childId;
    @Column(name = "invite_code",     nullable = false, unique = true) private String inviteCode;
    private String phone;
    @Column(name = "created_by", nullable = false) private UUID createdBy;

    /** active / used / revoked */
    @Column(nullable = false) private String status = "active";

    @Column(name = "used_at")    private Instant usedAt;
    @Column(name = "revoked_at") private Instant revokedAt;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
