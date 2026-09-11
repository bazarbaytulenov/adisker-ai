package kz.adisker.module.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {

    Optional<StoredFile> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<StoredFile> findByEntityTypeAndEntityIdOrderByCreatedAt(String entityType, UUID entityId);

    long countByEntityTypeAndEntityId(String entityType, UUID entityId);
}
