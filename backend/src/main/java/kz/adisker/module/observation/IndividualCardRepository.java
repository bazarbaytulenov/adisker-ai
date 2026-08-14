package kz.adisker.module.observation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IndividualCardRepository extends JpaRepository<IndividualCard, UUID> {
    Optional<IndividualCard> findByChildIdAndObservationId(UUID childId, UUID observationId);
    java.util.List<IndividualCard> findByChildIdAndDeletedFalse(UUID childId);
}
