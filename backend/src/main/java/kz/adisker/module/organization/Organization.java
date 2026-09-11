package kz.adisker.module.organization;

import jakarta.persistence.*;
import kz.adisker.common.entity.BaseEntity;
import lombok.*;

@Entity
@Table(name = "organizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "legal_name")
    private String legalName;

    private String bin;
    private String address;
    private String phone;
    private String email;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** Дневная ставка (тариф) для авто-начислений из табеля. */
    @Column(name = "daily_rate", nullable = false)
    private java.math.BigDecimal dailyRate = java.math.BigDecimal.ZERO;
}
