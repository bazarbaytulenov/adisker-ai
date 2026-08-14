package kz.adisker.module.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProspectivePlanRepository extends JpaRepository<ProspectivePlan, UUID> {
    Optional<ProspectivePlan> findByGroupIdAndAcademicYearAndMonthAndWeekAndLanguageAndDeletedFalse(
            UUID groupId, String year, int month, int week, String language);
    List<ProspectivePlan> findByGroupIdAndAcademicYearAndLanguageAndDeletedFalse(UUID groupId, String year, String lang);
    List<ProspectivePlan> findByBranchIdAndAcademicYearAndMonthAndLanguageAndDeletedFalse(UUID branchId, String year, int month, String lang);
}
