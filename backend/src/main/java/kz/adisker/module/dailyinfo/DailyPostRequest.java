package kz.adisker.module.dailyinfo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class DailyPostRequest {
    @NotNull private UUID branchId;
    @NotNull private UUID groupId;
    @NotNull private LocalDate postDate;
    private String theme;
    private String description;
    private String homeTasks;
}
