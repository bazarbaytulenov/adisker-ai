package kz.adisker.module.nomenclature;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/** Номенклатура дел (ТЗ 5.27): индекс, наименование, срок хранения. */
@Entity
@Table(name = "nomenclature")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Nomenclature {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "index_code") private String indexCode;
    @Column(nullable = false, columnDefinition = "TEXT") private String title;
    @Column(name = "retention_period") private String retentionPeriod;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(name = "sort_order", nullable = false) private int sortOrder = 0;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "is_deleted", nullable = false) private boolean deleted = false;

    @PrePersist void prePersist() { Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
