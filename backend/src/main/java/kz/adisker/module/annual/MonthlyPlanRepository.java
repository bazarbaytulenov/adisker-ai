package kz.adisker.module.annual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MonthlyPlanRepository extends JpaRepository<MonthlyPlan, UUID> {
    Optional<MonthlyPlan> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<MonthlyPlan> findByBranchIdAndYearAndMonthAndLanguage(UUID branchId, int year, int month, String lang);
    List<MonthlyPlan> findByOrganizationIdAndBranchIdOrderByYearDescMonthDesc(UUID orgId, UUID branchId);
}
