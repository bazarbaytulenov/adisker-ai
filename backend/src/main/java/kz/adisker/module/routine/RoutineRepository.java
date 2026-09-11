package kz.adisker.module.routine;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoutineRepository extends JpaRepository<Routine, UUID> {
    Optional<Routine> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Optional<Routine> findByGroupIdAndAcademicYearAndLanguage(UUID groupId, String year, String language);
    List<Routine> findByGroupIdOrderByAcademicYearDesc(UUID groupId);
}
