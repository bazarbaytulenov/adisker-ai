package kz.adisker.module.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class GroupRequest {
    @NotBlank private String name;
    private UUID branchId; // nullable — для организаций без филиалов
    private String language = "ru";
    private String groupType;
    private Integer ageFromMonths;
    private Integer ageToMonths;
    private String academicYear;
    private UUID educatorId;
    private String educatorPhone;
    private String educatorEmail;
    private String educatorInfo;
}
