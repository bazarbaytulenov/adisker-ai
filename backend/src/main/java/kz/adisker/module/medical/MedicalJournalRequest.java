package kz.adisker.module.medical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class MedicalJournalRequest {

    @NotNull
    private UUID branchId;

    @NotBlank
    private String journalType;

    @NotNull
    private LocalDate journalDate;

    private String data = "{}";  // JSON string

    private String notes;
}
