package kz.adisker.module.manager;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ManagerOrderRepository extends JpaRepository<ManagerOrder, UUID> {
    Optional<ManagerOrder> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID organizationId);
    List<ManagerOrder> findByOrganizationIdAndBranchIdAndDeletedFalseOrderByOrderDateDesc(UUID orgId, UUID branchId);
    List<ManagerOrder> findByOrganizationIdAndOrderTypeAndDeletedFalseOrderByOrderDateDesc(UUID orgId, String type);
}
