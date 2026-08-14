package kz.adisker.module.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {
    Optional<Schedule> findByGroupIdAndAcademicYearAndLanguageAndDeletedFalse(
            UUID groupId, String academicYear, String language);
}
