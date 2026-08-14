package kz.adisker.module.group;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "groups")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Group extends TenantEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(nullable = false)
    private String name;

    @Column(name = "age_from_months")
    private Integer ageFromMonths;

    @Column(name = "age_to_months")
    private Integer ageToMonths;

    @Column(nullable = false)
    private String language = "ru";

    @Column(name = "group_type")
    private String groupType;

    @Column(name = "educator_id")
    private UUID educatorId;

    @Column(name = "educator_phone")
    private String educatorPhone;

    @Column(name = "educator_email")
    private String educatorEmail;

    @Column(name = "educator_info")
    private String educatorInfo;

    @Column(name = "educator_photo_url")
    private String educatorPhotoUrl;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
