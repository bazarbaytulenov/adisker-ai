package kz.adisker.module.asset;

import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialAssetService {

    private final MaterialAssetRepository repo;

    public PageResponse<MaterialAssetDto> list(UUID orgId, UUID branchId, Pageable pageable) {
        var page = branchId != null
                ? repo.findByOrganizationIdAndBranchIdAndDeletedFalse(orgId, branchId, pageable)
                : repo.findByOrganizationIdAndDeletedFalse(orgId, pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    public MaterialAssetDto getById(UUID id, UUID orgId) {
        return toDto(repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaterialAsset", id)));
    }

    @Transactional
    public MaterialAssetDto create(UUID orgId, MaterialAssetRequest req) {
        MaterialAsset asset = buildFromRequest(orgId, req);
        return toDto(repo.save(asset));
    }

    @Transactional
    public MaterialAssetDto update(UUID id, UUID orgId, MaterialAssetRequest req) {
        MaterialAsset asset = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaterialAsset", id));
        applyRequest(asset, req);
        return toDto(repo.save(asset));
    }

    @Transactional
    public void delete(UUID id, UUID orgId) {
        MaterialAsset asset = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MaterialAsset", id));
        asset.setDeleted(true);
        asset.setDeletedAt(Instant.now());
        repo.save(asset);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MaterialAsset buildFromRequest(UUID orgId, MaterialAssetRequest req) {
        MaterialAsset asset = MaterialAsset.builder()
                .organizationId(orgId)
                .build();
        applyRequest(asset, req);
        return asset;
    }

    private void applyRequest(MaterialAsset asset, MaterialAssetRequest req) {
        asset.setBranchId(req.getBranchId());
        asset.setRoom(req.getRoom());
        asset.setAgeGroup(req.getAgeGroup());
        asset.setCategory(req.getCategory());
        asset.setName(req.getName());
        asset.setUnit(req.getUnit());
        asset.setNormQty(req.getNormQty());
        asset.setActualQty(req.getActualQty());
        asset.setWorkingQty(req.getWorkingQty());
        asset.setRepairQty(req.getRepairQty());
        asset.setWriteOffQty(req.getWriteOffQty());
        asset.setSupplyPct(req.getSupplyPct());
        asset.setInventoryNumber(req.getInventoryNumber());
        asset.setPurchaseDate(req.getPurchaseDate());
        asset.setResponsiblePerson(req.getResponsiblePerson());
        asset.setPurchasePlan(req.getPurchasePlan());
        asset.setPurchaseDeadline(req.getPurchaseDeadline());
        asset.setStatus(req.getStatus() != null ? req.getStatus() : "sufficient");
    }

    private MaterialAssetDto toDto(MaterialAsset a) {
        MaterialAssetDto dto = new MaterialAssetDto();
        dto.setId(a.getId());
        dto.setOrganizationId(a.getOrganizationId());
        dto.setBranchId(a.getBranchId());
        dto.setRoom(a.getRoom());
        dto.setAgeGroup(a.getAgeGroup());
        dto.setCategory(a.getCategory());
        dto.setName(a.getName());
        dto.setUnit(a.getUnit());
        dto.setNormQty(a.getNormQty());
        dto.setActualQty(a.getActualQty());
        dto.setWorkingQty(a.getWorkingQty());
        dto.setRepairQty(a.getRepairQty());
        dto.setWriteOffQty(a.getWriteOffQty());
        // compute shortage in Java (mirrors DB generated column)
        int shortage = a.getNormQty() != null ? Math.max(0, a.getNormQty() - a.getActualQty()) : 0;
        dto.setShortage(shortage);
        dto.setSupplyPct(a.getSupplyPct());
        dto.setInventoryNumber(a.getInventoryNumber());
        dto.setPurchaseDate(a.getPurchaseDate());
        dto.setResponsiblePerson(a.getResponsiblePerson());
        dto.setPurchasePlan(a.getPurchasePlan());
        dto.setPurchaseDeadline(a.getPurchaseDeadline());
        dto.setStatus(a.getStatus());
        dto.setCreatedAt(a.getCreatedAt());
        dto.setUpdatedAt(a.getUpdatedAt());
        return dto;
    }
}
