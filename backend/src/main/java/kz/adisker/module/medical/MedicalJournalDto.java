package kz.adisker.module.medical;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class MedicalJournalDto {
    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private String journalType;
    private LocalDate journalDate;
    private String data;   // Raw JSON string
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}
