package kz.adisker.module.group;

import lombok.Data;
import java.util.UUID;

@Data
public class GroupDto {
    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private String name;
    private Integer ageFromMonths;
    private Integer ageToMonths;
    private String language;
    private String groupType;
    private UUID educatorId;
    private String educatorPhone;
    private String educatorEmail;
    private String educatorInfo;
    private String educatorPhotoUrl;
    private String academicYear;
    private boolean active;
    private long activeChildrenCount;
}
