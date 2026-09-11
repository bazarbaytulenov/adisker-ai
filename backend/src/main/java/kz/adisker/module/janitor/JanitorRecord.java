package kz.adisker.module.janitor;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Запись хозяйственного модуля (ТЗ 5.23).
 * record_type: safety_briefing / incident / fire_safety / inventory.
 */
@Entity
@Table(name = "janitor_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JanitorRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;
    @Column(name = "record_type",     nullable = false) private String recordType;
    @Column(nullable = false) private String title;
    @Column(name = "record_date") private LocalDate recordDate;
    @Column(columnDefinition = "TEXT") private String responsible;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb") private String details = "{}";

    @Column(nullable = false) private String status = "active";
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "is_deleted", nullable = false) private boolean deleted = false;
    @Column(name = "deleted_at") private Instant deletedAt;

    @PrePersist void prePersist() { Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
