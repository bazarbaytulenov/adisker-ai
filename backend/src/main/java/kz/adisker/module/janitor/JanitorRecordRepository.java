package kz.adisker.module.janitor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JanitorRecordRepository extends JpaRepository<JanitorRecord, UUID> {
    Optional<JanitorRecord> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID organizationId);
    List<JanitorRecord> findByOrganizationIdAndBranchIdAndDeletedFalseOrderByRecordDateDesc(UUID orgId, UUID branchId);
    List<JanitorRecord> findByOrganizationIdAndRecordTypeAndDeletedFalseOrderByRecordDateDesc(UUID orgId, String type);
}
