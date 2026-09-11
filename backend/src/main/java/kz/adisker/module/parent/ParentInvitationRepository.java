package kz.adisker.module.parent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentInvitationRepository extends JpaRepository<ParentInvitation, UUID> {

    Optional<ParentInvitation> findByInviteCode(String inviteCode);

    Optional<ParentInvitation> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<ParentInvitation> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<ParentInvitation> findByChildIdOrderByCreatedAtDesc(UUID childId);

    boolean existsByInviteCode(String inviteCode);
}
