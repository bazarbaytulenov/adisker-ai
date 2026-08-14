package kz.adisker.module.observation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObservationRepository extends JpaRepository<Observation, UUID> {
    List<Observation> findByGroupIdAndAcademicYearAndPeriodAndDeletedFalse(UUID groupId, String year, String period);
    Optional<Observation> findByChildIdAndPeriodAndAcademicYear(UUID childId, String period, String year);
    List<Observation> findByChildIdAndDeletedFalse(UUID childId);
}

