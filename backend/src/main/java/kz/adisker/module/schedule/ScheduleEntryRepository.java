package kz.adisker.module.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ScheduleEntryRepository extends JpaRepository<ScheduleEntry, UUID> {
    List<ScheduleEntry> findByScheduleIdOrderByDayOfWeekAscStartTimeAsc(UUID scheduleId);
    void deleteByScheduleId(UUID scheduleId);
}
