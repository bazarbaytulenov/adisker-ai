package kz.adisker.module.asset;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class MaterialAssetDto {
    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private String room;
    private String ageGroup;
    private String category;
    private String name;
    private String unit;
    private Integer normQty;
    private int actualQty;
    private int workingQty;
    private int repairQty;
    private int writeOffQty;
    private int shortage;       // computed: max(0, normQty - actualQty)
    private Double supplyPct;
    private String inventoryNumber;
    private LocalDate purchaseDate;
    private String responsiblePerson;
    private String purchasePlan;
    private LocalDate purchaseDeadline;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
