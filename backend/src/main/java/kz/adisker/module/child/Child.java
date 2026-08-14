package kz.adisker.module.child;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "children")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Child extends TenantEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    private String gender;
    private String iin;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "admission_order_num")
    private String admissionOrderNum;

    @Column(name = "admission_order_date")
    private LocalDate admissionOrderDate;

    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    @Column(name = "discharge_order_num")
    private String dischargeOrderNum;

    @Column(name = "discharge_reason")
    private String dischargeReason;

    @Column(nullable = false)
    private String status = "active";

    private String notes;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "parent_phone")
    private String parentPhone;

    @Column(name = "parent_email")
    private String parentEmail;
}
