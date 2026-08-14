package kz.adisker.module.dailyinfo;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class DailyPostDto {
    private UUID id;
    private UUID organizationId;
    private UUID branchId;
    private UUID groupId;
    private LocalDate postDate;
    private String theme;
    private String description;
    private String homeTasks;
    private boolean published;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;

    static DailyPostDto from(DailyPost p) {
        return DailyPostDto.builder()
                .id(p.getId())
                .organizationId(p.getOrganizationId())
                .branchId(p.getBranchId())
                .groupId(p.getGroupId())
                .postDate(p.getPostDate())
                .theme(p.getTheme())
                .description(p.getDescription())
                .homeTasks(p.getHomeTasks())
                .published(p.isPublished())
                .publishedAt(p.getPublishedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
