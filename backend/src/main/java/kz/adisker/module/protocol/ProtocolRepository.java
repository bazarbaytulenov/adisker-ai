package kz.adisker.module.protocol;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, UUID> {
    Optional<Protocol> findByIdAndOrganizationId(UUID id, UUID organizationId);
    List<Protocol> findByOrganizationIdAndBranchIdOrderByProtocolDateDesc(UUID orgId, UUID branchId);
    List<Protocol> findByOrganizationIdAndProtocolTypeOrderByProtocolDateDesc(UUID orgId, String type);
}
