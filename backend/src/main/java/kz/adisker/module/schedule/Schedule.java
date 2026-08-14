package kz.adisker.module.schedule;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Entity
@Table(name = "schedules")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Schedule extends TenantEntity {
    @Column(name = "branch_id",     nullable = false) private UUID branchId;
    @Column(name = "group_id",      nullable = false) private UUID groupId;
    @Column(name = "academic_year", nullable = false, length = 9) private String academicYear;
    @Column(nullable = false, length = 2) private String language = "ru";
    @Column(name = "approval_info") private String approvalInfo;
    @Column(name = "is_published",  nullable = false) private boolean published = false;
    @Column(name = "published_at")  private java.time.Instant publishedAt;
}
