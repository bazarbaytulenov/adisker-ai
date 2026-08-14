package kz.adisker.module.observation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ObservationResultRepository extends JpaRepository<ObservationResult, UUID> {
    List<ObservationResult> findByObservationId(UUID observationId);
    void deleteByObservationIdAndIndicatorId(UUID observationId, UUID indicatorId);
}
