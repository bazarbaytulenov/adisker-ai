package kz.adisker.module.plan;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "prospective_plan_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "section_id",  nullable = false) private UUID sectionId;
    @Column(nullable = false) private int version;
    @Column(columnDefinition = "TEXT") private String content;
    @Column(columnDefinition = "TEXT") private String objectives;
    @Column(columnDefinition = "TEXT") private String materials;
    @Column(name = "changed_by", nullable = false) private UUID changedBy;
    @Column(name = "changed_at", nullable = false) private Instant changedAt;
    @Column(name = "change_comment") private String changeComment;
}
