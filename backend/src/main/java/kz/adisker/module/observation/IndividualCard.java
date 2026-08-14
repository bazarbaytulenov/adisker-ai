package kz.adisker.module.observation;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.util.UUID;

@Entity @Table(name = "individual_cards")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class IndividualCard extends TenantEntity {
    @Column(name = "child_id",       nullable = false) private UUID childId;
    @Column(name = "observation_id", nullable = false) private UUID observationId;
    @Column(name = "game_id")        private UUID gameId;
    @Column(name = "game_name")      private String gameName;
    @Column(name = "game_objectives", columnDefinition = "TEXT") private String gameObjectives;
    @Column(name = "game_procedure",  columnDefinition = "TEXT") private String gameProcedure;
    @Column(name = "custom_notes",    columnDefinition = "TEXT") private String customNotes;
    @Column(nullable = false) private String language = "ru";
}
