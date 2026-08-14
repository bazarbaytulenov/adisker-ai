package kz.adisker.module.child;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChildRepository extends JpaRepository<Child, UUID> {
    Page<Child> findByOrganizationIdAndDeletedFalse(UUID orgId, Pageable pageable);
    Page<Child> findByOrganizationIdAndBranchIdAndDeletedFalse(UUID orgId, UUID branchId, Pageable pageable);
    Page<Child> findByOrganizationIdAndGroupIdAndDeletedFalse(UUID orgId, UUID groupId, Pageable pageable);
    Page<Child> findByOrganizationIdAndBranchIdAndGroupIdAndDeletedFalse(UUID orgId, UUID branchId, UUID groupId, Pageable pageable);
    Optional<Child> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID orgId);
    long countByGroupIdAndStatusAndDeletedFalse(UUID groupId, String status);
}
