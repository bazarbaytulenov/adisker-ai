package kz.adisker.module.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlanVersionRepository extends JpaRepository<PlanVersion, UUID> {

    /** История версий секции в порядке убывания номера версии. */
    List<PlanVersion> findBySectionIdOrderByVersionDesc(UUID sectionId);
}
