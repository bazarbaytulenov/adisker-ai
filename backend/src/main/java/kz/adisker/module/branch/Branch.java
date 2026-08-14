package kz.adisker.module.branch;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "branches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Branch extends TenantEntity {

    @Column(nullable = false)
    private String name;

    private String address;
    private String phone;

    @Column(name = "head_name")
    private String headName;

    @Column(name = "design_capacity")
    private Integer designCapacity;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
