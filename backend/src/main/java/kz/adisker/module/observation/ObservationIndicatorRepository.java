package kz.adisker.module.observation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservationIndicatorRepository extends JpaRepository<ObservationIndicator, UUID> {
    List<ObservationIndicator> findByOrganizationIdAndAgeGroupAndActiveTrue(UUID orgId, String ageGroup);
    List<ObservationIndicator> findByOrganizationIdAndActiveTrueOrderBySortOrder(UUID orgId);
}
