package kz.adisker.module.protocol;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Протокол совета (ТЗ 5.10). Типы: pedagogical / methodical / parents / guardian / ethics.
 * Пункты (heard / speakers / decisions) хранятся как JSONB-массивы.
 */
@Entity
@Table(name = "protocols")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Protocol {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;

    @Column(name = "protocol_type", nullable = false) private String protocolType;
    private String number;
    @Column(name = "protocol_date", nullable = false) private LocalDate protocolDate;
    @Column(nullable = false) private String language = "ru";

    @Column(columnDefinition = "TEXT") private String chairman;
    @Column(columnDefinition = "TEXT") private String secretary;
    @Column(columnDefinition = "TEXT") private String attendees;
    @Column(columnDefinition = "TEXT") private String agenda;

    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String heard;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String speakers;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "jsonb") private String decisions;

    @Column(nullable = false) private String status = "draft";
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "updated_by") private UUID updatedBy;

    @PrePersist void prePersist() { Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
