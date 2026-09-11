package kz.adisker.module.annual;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MonthlyPlanEventRepository extends JpaRepository<MonthlyPlanEvent, UUID> {
    List<MonthlyPlanEvent> findByMonthlyPlanId(UUID monthlyPlanId);
}
