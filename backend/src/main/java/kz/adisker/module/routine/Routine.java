package kz.adisker.module.routine;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Режим дня группы (ТЗ 5.18). Отдельные документы ru/kk.
 * items: JSONB-массив [{time, activity}].
 */
@Entity
@Table(name = "routines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Routine {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;
    @Column(name = "group_id",        nullable = false) private UUID groupId;
    @Column(name = "academic_year",   nullable = false) private String academicYear;
    @Column(nullable = false) private String language = "ru";
    @Column(name = "approval_info", columnDefinition = "TEXT") private String approvalInfo;
    @Column(name = "group_name") private String groupName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb") private String items = "[]";

    @Column(name = "is_published", nullable = false) private boolean published = false;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;

    @PrePersist void prePersist() { Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
