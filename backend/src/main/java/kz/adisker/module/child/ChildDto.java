package kz.adisker.module.child;

import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ChildDto {
    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private UUID groupId;
    private String lastName;
    private String firstName;
    private String middleName;
    private LocalDate birthDate;
    private String gender;
    private String iin;
    private String photoUrl;
    private LocalDate admissionDate;
    private String admissionOrderNum;
    private LocalDate dischargeDate;
    private String dischargeReason;
    private String status;
    private String notes;
    private String parentName;
    private String parentPhone;
    private String parentEmail;
}
