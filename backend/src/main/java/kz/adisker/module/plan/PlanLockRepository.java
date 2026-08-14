package kz.adisker.module.plan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlanLockRepository extends JpaRepository<PlanLock, UUID> {
    Optional<PlanLock> findBySectionId(UUID sectionId);
    @Modifying @Query("DELETE FROM PlanLock l WHERE l.expiresAt < :now")
    void deleteExpired(Instant now);
}
