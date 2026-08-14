package kz.adisker.module.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanSectionRepository extends JpaRepository<PlanSection, UUID> {
    List<PlanSection> findByPlanIdAndDeletedFalseOrderBySortOrder(UUID planId);
    Optional<PlanSection> findByPlanIdAndDomainAndDeletedFalse(UUID planId, String domain);
}
