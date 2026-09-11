package kz.adisker.module.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndActionOrderByCreatedAtDesc(
            UUID organizationId, String action, Pageable pageable);

    Page<AuditLog> findByOrganizationIdAndEntityTypeOrderByCreatedAtDesc(
            UUID organizationId, String entityType, Pageable pageable);
}
