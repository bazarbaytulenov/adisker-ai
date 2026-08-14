package kz.adisker.module.observation;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "observation_indicators")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ObservationIndicator {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "age_group", nullable = false) private String ageGroup;
    @Column(nullable = false) private String domain;
    @Column(nullable = false) private String criterion;
    @Column(nullable = false) private String indicator;
    @Column(name = "sort_order") private int sortOrder;
    @Column(name = "is_active") private boolean active = true;
    @Column(name = "created_at") private Instant createdAt;
    @Column(name = "updated_at") private Instant updatedAt;
}
