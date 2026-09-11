package kz.adisker.module.plan;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Закрепление специалиста (педагог казахского, музыкальный руководитель,
 * инструктор по физкультуре) за конкретной группой.
 * Соответствует таблице specialist_group_assignments из V1.
 */
@Entity
@Table(name = "specialist_group_assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SpecialistGroupAssignment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "specialist_id",   nullable = false) private UUID specialistId;
    @Column(name = "group_id",        nullable = false) private UUID groupId;
    @Column(name = "branch_id",       nullable = false) private UUID branchId;
    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "assigned_at",     nullable = false) private Instant assignedAt;
}
