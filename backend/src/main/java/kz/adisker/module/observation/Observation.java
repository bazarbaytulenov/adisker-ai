package kz.adisker.module.observation;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Entity @Table(name = "observations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Observation extends TenantEntity {
    @Column(name = "branch_id", nullable = false) private UUID branchId;
    @Column(name = "group_id",  nullable = false) private UUID groupId;
    @Column(name = "child_id",  nullable = false) private UUID childId;
    @Column(nullable = false) private String period; // start / mid / final
    @Column(name = "academic_year", nullable = false) private String academicYear;
    @Column(name = "filled_by") private UUID filledBy;
    @Column(name = "is_complete", nullable = false) private boolean complete = false;
}
