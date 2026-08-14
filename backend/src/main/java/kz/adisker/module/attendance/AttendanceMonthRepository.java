package kz.adisker.module.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceMonthRepository extends JpaRepository<AttendanceMonth, UUID> {
    Optional<AttendanceMonth> findByGroupIdAndYearAndMonth(UUID groupId, int year, int month);
    List<AttendanceMonth> findByOrganizationIdAndYearAndMonth(UUID orgId, int year, int month);
}
