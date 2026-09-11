package kz.adisker.module.annual;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "annual_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnualPlan {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;
    @Column(name = "academic_year",   nullable = false) private String academicYear;
    @Column(nullable = false) private String language = "ru";
    private String title;
    @Column(nullable = false) private String status = "draft"; // draft / approved
    @Column(name = "approved_by") private UUID approvedBy;
    @Column(name = "approved_at") private Instant approvedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "created_by") private UUID createdBy;

    @PrePersist void prePersist() { Instant n = Instant.now(); if (createdAt == null) createdAt = n; updatedAt = n; }
    @PreUpdate  void preUpdate()  { updatedAt = Instant.now(); }
}
