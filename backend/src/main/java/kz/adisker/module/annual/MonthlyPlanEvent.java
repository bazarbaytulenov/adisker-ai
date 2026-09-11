package kz.adisker.module.annual;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "monthly_plan_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MonthlyPlanEvent {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "monthly_plan_id", nullable = false) private UUID monthlyPlanId;
    @Column(name = "annual_event_id") private UUID annualEventId;
    @Column(nullable = false, columnDefinition = "TEXT") private String title;
    @Column(name = "event_form") private String eventForm;
    @Column(columnDefinition = "TEXT") private String participants;
    @Column(name = "event_date") private LocalDate eventDate;
    @Column(columnDefinition = "TEXT") private String responsible;
    @Column(columnDefinition = "TEXT") private String notes;
}
