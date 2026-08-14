package kz.adisker.module.cyclogram;

import lombok.Data;

import java.util.UUID;

@Data
public class CyclogramRequest {
    private UUID organizationId;
    private UUID branchId;
    private UUID groupId;
    private String academicYear;
    private int month;
    private int week;
    private String language;
    /** JSONB content: day-by-day schedule as JSON string */
    private String content;
    private String status;
}
