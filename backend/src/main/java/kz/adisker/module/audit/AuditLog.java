package kz.adisker.module.audit;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @Column(name = "organization_id") private UUID organizationId;
    @Column(name = "branch_id")       private UUID branchId;
    @Column(name = "user_id")         private UUID userId;
    @Column(name = "user_email")      private String userEmail;

    /** LOGIN / LOGOUT / CREATE / UPDATE / DELETE / PRINT / EXPORT / APPROVE / REJECT */
    @Column(nullable = false) private String action;

    @Column(name = "entity_type") private String entityType;
    @Column(name = "entity_id")   private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb") private String oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb") private String newValues;

    @Column(name = "ip_address") private String ipAddress;
    @Column(name = "user_agent", columnDefinition = "TEXT") private String userAgent;
    @Column(columnDefinition = "TEXT") private String notes;

    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
