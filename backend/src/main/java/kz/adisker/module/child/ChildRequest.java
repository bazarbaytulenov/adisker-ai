package kz.adisker.module.child;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class ChildRequest {
    @NotBlank private String lastName;
    @NotBlank private String firstName;
    private String middleName;
    @NotNull private LocalDate birthDate;
    private String gender;
    private String iin;
    @NotNull private UUID branchId;
    private UUID groupId;
    private LocalDate admissionDate;
    private String admissionOrderNum;
    private String parentName;
    private String parentPhone;
    private String parentEmail;
    private String notes;
}
