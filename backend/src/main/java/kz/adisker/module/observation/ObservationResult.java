package kz.adisker.module.observation;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "observation_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObservationResult {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "observation_id", nullable = false) private UUID observationId;
    @Column(name = "indicator_id",   nullable = false) private UUID indicatorId;
    private String level; // В / С / Н
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
