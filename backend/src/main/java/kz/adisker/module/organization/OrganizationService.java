package kz.adisker.module.organization;

import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.branch.Branch;
import kz.adisker.module.branch.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationRepository repo;
    private final BranchRepository branchRepository;

    public PageResponse<OrganizationDto> getAll(Pageable pageable) {
        return PageResponse.from(repo.findByDeletedFalse(pageable).map(this::toDto));
    }

    public OrganizationDto getById(UUID id) {
        return toDto(findOrThrow(id));
    }

    @Transactional
    public OrganizationDto create(OrganizationRequest req) {
        Organization org = Organization.builder()
                .name(req.getName())
                .legalName(req.getLegalName())
                .bin(req.getBin())
                .address(req.getAddress())
                .phone(req.getPhone())
                .email(req.getEmail())
                .logoUrl(req.getLogoUrl())
                .active(true)
                .dailyRate(req.getDailyRate() != null ? req.getDailyRate() : java.math.BigDecimal.ZERO)
                .build();
        Organization saved = repo.save(org);

        // Автоматически создаём дефолтный филиал для новой организации
        Branch defaultBranch = Branch.builder()
                .name("Основной")
                .active(true)
                .isDefault(true)
                .build();
        defaultBranch.setOrganizationId(saved.getId());
        branchRepository.save(defaultBranch);

        return toDto(saved);
    }

    @Transactional
    public OrganizationDto update(UUID id, OrganizationRequest req) {
        Organization org = findOrThrow(id);
        org.setName(req.getName());
        org.setLegalName(req.getLegalName());
        org.setBin(req.getBin());
        org.setAddress(req.getAddress());
        org.setPhone(req.getPhone());
        org.setEmail(req.getEmail());
        org.setLogoUrl(req.getLogoUrl());
        if (req.getDailyRate() != null) org.setDailyRate(req.getDailyRate());
        return toDto(repo.save(org));
    }

    @Transactional
    public void delete(UUID id) {
        Organization org = findOrThrow(id);
        org.setDeleted(true);
        org.setDeletedAt(Instant.now());
        repo.save(org);
    }

    private Organization findOrThrow(UUID id) {
        return repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", id));
    }

    private OrganizationDto toDto(Organization org) {
        OrganizationDto dto = new OrganizationDto();
        dto.setId(org.getId());
        dto.setName(org.getName());
        dto.setLegalName(org.getLegalName());
        dto.setBin(org.getBin());
        dto.setAddress(org.getAddress());
        dto.setPhone(org.getPhone());
        dto.setEmail(org.getEmail());
        dto.setLogoUrl(org.getLogoUrl());
        dto.setActive(org.isActive());
        dto.setDailyRate(org.getDailyRate());
        dto.setCreatedAt(org.getCreatedAt());
        return dto;
    }
}
