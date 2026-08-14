package kz.adisker.module.dailyinfo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyPostRepository extends JpaRepository<DailyPost, UUID> {
    Optional<DailyPost> findByGroupIdAndPostDateAndDeletedFalse(UUID groupId, LocalDate date);
    List<DailyPost> findByGroupIdAndPostDateBetweenAndDeletedFalseOrderByPostDateDesc(
            UUID groupId, LocalDate from, LocalDate to);
    List<DailyPost> findByOrganizationIdAndBranchIdAndPostDateAndDeletedFalse(
            UUID orgId, UUID branchId, LocalDate date);
}
