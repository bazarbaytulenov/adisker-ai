package kz.adisker.module.annual;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity @Table(name = "annual_plan_sections")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnualPlanSection {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "annual_plan_id", nullable = false) private UUID annualPlanId;
    @Column(nullable = false) private String title;
    @Column(name = "sort_order", nullable = false) private int sortOrder = 0;
}
