package kz.adisker.module.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailAndDeletedFalse(String email);
    Optional<User> findByIdAndDeletedFalse(UUID id);
    Page<User> findByOrganizationIdAndDeletedFalse(UUID orgId, Pageable pageable);
    List<User> findByOrganizationIdAndRoleCodeAndDeletedFalse(UUID orgId, String roleCode);
    boolean existsByEmail(String email);
}
