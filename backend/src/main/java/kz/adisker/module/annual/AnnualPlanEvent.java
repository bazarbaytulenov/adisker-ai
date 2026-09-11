package kz.adisker.module.annual;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "annual_plan_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnnualPlanEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "section_id",      nullable = false) private UUID sectionId;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(nullable = false, columnDefinition = "TEXT") private String title;
    @Column(name = "event_form") private String eventForm;
    @Column(columnDefinition = "TEXT") private String participants;
    private LocalDate deadline;
    @Column(columnDefinition = "TEXT") private String responsible;
    @Column(columnDefinition = "TEXT") private String notes;
    private Integer month; // месяц для авто-передачи в месячный план
    @Column(name = "is_propagated", nullable = false) private boolean propagated = false;
}
