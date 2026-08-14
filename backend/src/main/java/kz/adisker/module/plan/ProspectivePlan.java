package kz.adisker.module.plan;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name = "prospective_plans")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class ProspectivePlan extends TenantEntity {
    @Column(name = "branch_id", nullable = false) private UUID branchId;
    @Column(name = "group_id",  nullable = false) private UUID groupId;
    @Column(name = "academic_year", nullable = false) private String academicYear;
    @Column(nullable = false) private int month;
    @Column(nullable = false) private int week;
    private String theme;
    @Column(nullable = false) private String language = "ru";
    @Column(name = "overall_status", nullable = false) private String overallStatus = "draft";
    @Column(name = "fill_pct") private BigDecimal fillPct = BigDecimal.ZERO;
}
