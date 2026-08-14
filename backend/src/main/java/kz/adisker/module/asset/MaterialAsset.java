package kz.adisker.module.asset;

import jakarta.persistence.*;
import kz.adisker.common.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "material_assets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class MaterialAsset extends BaseEntity {

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    private String room;

    @Column(name = "age_group")
    private String ageGroup;

    private String category;

    @Column(nullable = false, length = 512)
    private String name;

    private String unit;

    @Column(name = "norm_qty")
    private Integer normQty;

    @Column(name = "actual_qty", nullable = false)
    private int actualQty = 0;

    @Column(name = "working_qty", nullable = false)
    private int workingQty = 0;

    @Column(name = "repair_qty", nullable = false)
    private int repairQty = 0;

    @Column(name = "write_off_qty", nullable = false)
    private int writeOffQty = 0;

    @Column(name = "supply_pct")
    private Double supplyPct;

    @Column(name = "inventory_number", length = 100)
    private String inventoryNumber;

    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @Column(name = "responsible_person", length = 255)
    private String responsiblePerson;

    @Column(name = "purchase_plan")
    private String purchasePlan;

    @Column(name = "purchase_deadline")
    private LocalDate purchaseDeadline;

    /**
     * Status: sufficient / insufficient / absent
     */
    @Column(nullable = false, length = 30)
    private String status = "sufficient";
}
