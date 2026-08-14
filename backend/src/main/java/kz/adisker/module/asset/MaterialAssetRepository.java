package kz.adisker.module.asset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaterialAssetRepository extends JpaRepository<MaterialAsset, UUID> {

    Page<MaterialAsset> findByOrganizationIdAndBranchIdAndDeletedFalse(
            UUID orgId, UUID branchId, Pageable pageable);

    Page<MaterialAsset> findByOrganizationIdAndDeletedFalse(UUID orgId, Pageable pageable);

    List<MaterialAsset> findByOrganizationIdAndBranchIdAndCategoryAndDeletedFalse(
            UUID orgId, UUID branchId, String category);

    Optional<MaterialAsset> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID orgId);
}
