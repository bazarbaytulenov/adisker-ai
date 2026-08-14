package kz.adisker.module.plan;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "prospective_plan_locks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanLock {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "section_id", nullable = false, unique = true) private UUID sectionId;
    @Column(name = "locked_by", nullable = false)  private UUID lockedBy;
    @Column(name = "locked_at", nullable = false)  private Instant lockedAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
}
