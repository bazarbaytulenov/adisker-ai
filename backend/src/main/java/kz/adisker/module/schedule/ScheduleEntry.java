package kz.adisker.module.schedule;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "schedule_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleEntry {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "schedule_id",  nullable = false) private UUID scheduleId;
    @Column(name = "day_of_week",  nullable = false) private int dayOfWeek; // 1=Пн..5=Пт
    @Column(name = "start_time",   nullable = false) private LocalTime startTime;
    @Column(name = "end_time")                       private LocalTime endTime;
    @Column(nullable = false)                        private String subject;
    @Column(name = "educator_id")                    private UUID educatorId;
    @Column(name = "educator_role")                  private String educatorRole;
    private String notes;
}
