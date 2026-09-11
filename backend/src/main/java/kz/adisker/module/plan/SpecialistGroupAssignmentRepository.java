package kz.adisker.module.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SpecialistGroupAssignmentRepository
        extends JpaRepository<SpecialistGroupAssignment, UUID> {

    boolean existsBySpecialistIdAndGroupId(UUID specialistId, UUID groupId);
}
