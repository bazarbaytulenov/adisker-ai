package kz.adisker.module.parent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentAccountRepository extends JpaRepository<ParentAccount, UUID> {

    List<ParentAccount> findByUserIdAndActiveTrue(UUID userId);

    Optional<ParentAccount> findByUserIdAndChildId(UUID userId, UUID childId);

    boolean existsByUserIdAndChildId(UUID userId, UUID childId);
}
