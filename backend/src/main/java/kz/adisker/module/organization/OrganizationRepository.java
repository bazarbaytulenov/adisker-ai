package kz.adisker.module.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    Page<Organization> findByDeletedFalse(Pageable pageable);
    Optional<Organization> findByIdAndDeletedFalse(UUID id);
    boolean existsByBin(String bin);
}
