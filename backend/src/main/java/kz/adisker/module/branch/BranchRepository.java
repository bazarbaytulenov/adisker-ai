package kz.adisker.module.branch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BranchRepository extends JpaRepository<Branch, UUID> {
    Page<Branch> findByOrganizationIdAndDeletedFalse(UUID orgId, Pageable pageable);
    List<Branch> findByOrganizationIdAndDeletedFalseAndActiveTrue(UUID orgId);
    Optional<Branch> findByIdAndDeletedFalse(UUID id);
    Optional<Branch> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID orgId);
}
