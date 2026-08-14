package kz.adisker.module.group;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {
    Page<Group> findByOrganizationIdAndBranchIdAndDeletedFalse(UUID orgId, UUID branchId, Pageable pageable);
    List<Group> findByOrganizationIdAndDeletedFalseAndActiveTrue(UUID orgId);
    List<Group> findByBranchIdAndDeletedFalseAndActiveTrue(UUID branchId);
    Optional<Group> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID orgId);
}
