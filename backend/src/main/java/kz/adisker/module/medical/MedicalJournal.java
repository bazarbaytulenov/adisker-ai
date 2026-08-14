package kz.adisker.module.medical;

import jakarta.persistence.*;
import kz.adisker.common.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "medical_journals")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class MedicalJournal extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    /**
     * Types: menu / vaccination / height_weight / brakerage / daily_sample / fridge
     */
    @Column(name = "journal_type", nullable = false, length = 50)
    private String journalType;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    /**
     * JSONB: flexible data depending on journal type.
     * Stored as JSON string, deserialized on the service layer.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String data;

    private String notes;
}
