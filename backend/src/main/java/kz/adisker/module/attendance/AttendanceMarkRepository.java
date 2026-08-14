package kz.adisker.module.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceMarkRepository extends JpaRepository<AttendanceMark, UUID> {
    List<AttendanceMark> findByAttendanceMonthId(UUID monthId);
    List<AttendanceMark> findByAttendanceMonthIdAndChildId(UUID monthId, UUID childId);

    @Modifying
    @Query("DELETE FROM AttendanceMark m WHERE m.attendanceMonthId = :monthId AND m.childId = :childId AND m.day = :day")
    void deleteByMonthAndChildAndDay(UUID monthId, UUID childId, int day);
}
