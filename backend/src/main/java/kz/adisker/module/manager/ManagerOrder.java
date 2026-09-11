package kz.adisker.module.manager;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Приказ руководителя (ТЗ 5.24). Типы: приём/перевод/выбытие/выпуск детей,
 * приём/перевод/увольнение сотрудников.
 */
@Entity
@Table(name = "manager_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ManagerOrder {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;

    /** child_admission / child_transfer / child_discharge / child_graduation /
        staff_hire / staff_transfer / staff_dismiss */
    @Column(name = "order_type",   nullable = false) private String orderType;
    @Column(name = "order_number") private String orderNumber;
    @Column(name = "order_date",   nullable = false) private LocalDate orderDate;
    @Column(nullable = false, columnDefinition = "TEXT") private String subject;
    @Column(columnDefinition = "TEXT") private String content;

    @Column(name = "related_child_id") private UUID relatedChildId;
    @Column(name = "related_user_id")  private UUID relatedUserId;

    @Column(nullable = false) private String language = "ru";
    @Column(nullable = false) private String status = "draft";
    @Column(name = "signed_by") private UUID signedBy;
    @Column(name = "signed_at") private Instant signedAt;
    @Column(name = "file_url")  private String fileUrl;

    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;
    @Column(name = "is_deleted", nullable = false) private boolean deleted = false;

    @PrePersist void prePersist() { Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
