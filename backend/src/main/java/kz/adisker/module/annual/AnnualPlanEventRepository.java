package kz.adisker.module.annual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnualPlanEventRepository extends JpaRepository<AnnualPlanEvent, UUID> {
    List<AnnualPlanEvent> findBySectionId(UUID sectionId);
    List<AnnualPlanEvent> findByOrganizationIdAndMonthAndPropagatedFalse(UUID orgId, int month);
}
