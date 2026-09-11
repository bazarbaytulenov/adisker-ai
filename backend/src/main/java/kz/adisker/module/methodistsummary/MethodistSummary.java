package kz.adisker.module.methodistsummary;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "methodist_summaries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MethodistSummary {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;
    @Column(name = "group_id")                          private UUID groupId;
    @Column(nullable = false)                           private String period;
    @Column(name = "academic_year",   nullable = false) private String academicYear;
    @Column(nullable = false)                           private String domain;
    @Column(name = "age_group")                         private String ageGroup;

    @Column(name = "total_children",  nullable = false) private int totalChildren;
    @Column(name = "high_count",      nullable = false) private int highCount;
    @Column(name = "mid_count",       nullable = false) private int midCount;
    @Column(name = "low_count",       nullable = false) private int lowCount;

    @Column(name = "high_pct")    private BigDecimal highPct;
    @Column(name = "mid_pct")     private BigDecimal midPct;
    @Column(name = "low_pct")     private BigDecimal lowPct;

    @Column(name = "calculated_at") private Instant calculatedAt;
    @Column(name = "created_at")    private Instant createdAt;
    @Column(name = "updated_at")    private Instant updatedAt;
}
