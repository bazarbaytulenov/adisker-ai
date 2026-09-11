package kz.adisker.module.nomenclature;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NomenclatureRepository extends JpaRepository<Nomenclature, UUID> {
    Optional<Nomenclature> findByIdAndOrganizationIdAndDeletedFalse(UUID id, UUID organizationId);
    List<Nomenclature> findByOrganizationIdAndDeletedFalseOrderBySortOrder(UUID organizationId);
    List<Nomenclature> findByOrganizationIdAndDeletedFalseAndTitleContainingIgnoreCaseOrderBySortOrder(
            UUID organizationId, String title);
}
