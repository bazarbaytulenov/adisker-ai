package kz.adisker.module.asset;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class MaterialAssetRequest {

    @NotNull
    private UUID branchId;

    private String room;
    private String ageGroup;
    private String category;

    @NotBlank
    private String name;

    private String unit;
    private Integer normQty;

    @Min(0)
    private int actualQty = 0;

    @Min(0)
    private int workingQty = 0;

    @Min(0)
    private int repairQty = 0;

    @Min(0)
    private int writeOffQty = 0;

    private Double supplyPct;
    private String inventoryNumber;
    private LocalDate purchaseDate;
    private String responsiblePerson;
    private String purchasePlan;
    private LocalDate purchaseDeadline;
    private String status = "sufficient";
}
