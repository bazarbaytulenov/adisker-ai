package kz.adisker.module.dailyinfo;

import jakarta.persistence.*;
import kz.adisker.common.entity.TenantEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class DailyPost extends TenantEntity {
    @Column(name = "branch_id",   nullable = false) private UUID branchId;
    @Column(name = "group_id",    nullable = false) private UUID groupId;
    @Column(name = "post_date",   nullable = false) private LocalDate postDate;
    private String theme;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "home_tasks",  columnDefinition = "TEXT") private String homeTasks;
    @Column(name = "is_published", nullable = false) private boolean published = false;
    @Column(name = "published_at") private Instant publishedAt;
}
