package kz.adisker.module.branch;

import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchService {

    private final BranchRepository repo;

    public PageResponse<BranchDto> getByOrg(UUID orgId, Pageable pageable) {
        return PageResponse.from(repo.findByOrganizationIdAndDeletedFalse(orgId, pageable).map(this::toDto));
    }

    public List<BranchDto> getAllActiveByOrg(UUID orgId) {
        return repo.findByOrganizationIdAndDeletedFalseAndActiveTrue(orgId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public BranchDto getById(UUID id, UUID orgId) {
        return toDto(repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id)));
    }

    @Transactional
    public BranchDto create(UUID orgId, BranchRequest req) {
        Branch branch = Branch.builder()
                .name(req.getName())
                .address(req.getAddress())
                .phone(req.getPhone())
                .headName(req.getHeadName())
                .designCapacity(req.getDesignCapacity())
                .active(true)
                .build();
        branch.setOrganizationId(orgId);
        return toDto(repo.save(branch));
    }

    @Transactional
    public BranchDto update(UUID id, UUID orgId, BranchRequest req) {
        Branch branch = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        branch.setName(req.getName());
        branch.setAddress(req.getAddress());
        branch.setPhone(req.getPhone());
        branch.setHeadName(req.getHeadName());
        branch.setDesignCapacity(req.getDesignCapacity());
        return toDto(repo.save(branch));
    }

    @Transactional
    public void delete(UUID id, UUID orgId) {
        Branch branch = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Branch", id));
        branch.setDeleted(true);
        branch.setDeletedAt(Instant.now());
        repo.save(branch);
    }

    private BranchDto toDto(Branch b) {
        BranchDto dto = new BranchDto();
        dto.setId(b.getId());
        dto.setOrganizationId(b.getOrganizationId());
        dto.setName(b.getName());
        dto.setAddress(b.getAddress());
        dto.setPhone(b.getPhone());
        dto.setHeadName(b.getHeadName());
        dto.setDesignCapacity(b.getDesignCapacity());
        dto.setActive(b.isActive());
        dto.setCreatedAt(b.getCreatedAt());
        return dto;
    }
}
