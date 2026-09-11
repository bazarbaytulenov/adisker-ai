package kz.adisker.module.annual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnualPlanRepository extends JpaRepository<AnnualPlan, UUID> {
    Optional<AnnualPlan> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<AnnualPlan> findByOrganizationIdAndBranchIdOrderByAcademicYearDesc(UUID orgId, UUID branchId);
    Optional<AnnualPlan> findByBranchIdAndAcademicYearAndLanguage(UUID branchId, String year, String lang);
}
