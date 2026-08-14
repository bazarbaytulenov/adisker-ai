package kz.adisker.module.chat;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {

    List<ChatThread> findByOrganizationIdAndEducatorIdAndActiveTrue(UUID orgId, UUID educatorId);

    List<ChatThread> findByOrganizationIdAndParentUserIdAndActiveTrue(UUID orgId, UUID parentUserId);

    Optional<ChatThread> findByChildIdAndEducatorId(UUID childId, UUID educatorId);

    Optional<ChatThread> findByIdAndOrganizationId(UUID id, UUID orgId);
}
