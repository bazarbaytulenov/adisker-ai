package kz.adisker.module.library;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LibraryFundRepository extends JpaRepository<LibraryFund, UUID> {

    Page<LibraryFund> findByOrganizationIdAndBranchIdAndDeletedFalse(UUID orgId, UUID branchId, Pageable pageable);

    Page<LibraryFund> findByOrganizationIdAndDeletedFalse(UUID orgId, Pageable pageable);

    Optional<LibraryFund> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID orgId);
}
