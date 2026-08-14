package kz.adisker.module.library;

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
public class LibraryFundService {

    private final LibraryFundRepository repo;

    public PageResponse<LibraryFundDto> list(UUID orgId, UUID branchId, Pageable pageable) {
        var page = branchId != null
                ? repo.findByOrganizationIdAndBranchIdAndDeletedFalse(orgId, branchId, pageable)
                : repo.findByOrganizationIdAndDeletedFalse(orgId, pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    public LibraryFundDto getById(UUID id, UUID orgId) {
        return toDto(repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("LibraryFund", id)));
    }

    @Transactional
    public LibraryFundDto create(UUID orgId, LibraryFundRequest req) {
        LibraryFund fund = LibraryFund.builder()
                .organizationId(orgId)
                .branchId(req.getBranchId())
                .ageGroup(req.getAgeGroup())
                .language(req.getLanguage() != null ? req.getLanguage() : "ru")
                .publicationType(req.getPublicationType())
                .title(req.getTitle())
                .authors(req.getAuthors())
                .publisher(req.getPublisher())
                .publishYear(req.getPublishYear())
                .inventoryNumber(req.getInventoryNumber())
                .quantity(req.getQuantity() != null ? req.getQuantity() : 1)
                .receiptDate(req.getReceiptDate())
                .electronic(req.isElectronic())
                .orderNumber(req.getOrderNumber())
                .listItem(req.getListItem())
                .checkDate(req.getCheckDate())
                .docLink(req.getDocLink())
                .condition(req.getCondition() != null ? req.getCondition() : "good")
                .notes(req.getNotes())
                .build();
        return toDto(repo.save(fund));
    }

    @Transactional
    public LibraryFundDto update(UUID id, UUID orgId, LibraryFundRequest req) {
        LibraryFund fund = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("LibraryFund", id));
        fund.setBranchId(req.getBranchId());
        fund.setAgeGroup(req.getAgeGroup());
        fund.setLanguage(req.getLanguage() != null ? req.getLanguage() : "ru");
        fund.setPublicationType(req.getPublicationType());
        fund.setTitle(req.getTitle());
        fund.setAuthors(req.getAuthors());
        fund.setPublisher(req.getPublisher());
        fund.setPublishYear(req.getPublishYear());
        fund.setInventoryNumber(req.getInventoryNumber());
        fund.setQuantity(req.getQuantity() != null ? req.getQuantity() : 1);
        fund.setReceiptDate(req.getReceiptDate());
        fund.setElectronic(req.isElectronic());
        fund.setOrderNumber(req.getOrderNumber());
        fund.setListItem(req.getListItem());
        fund.setCheckDate(req.getCheckDate());
        fund.setDocLink(req.getDocLink());
        fund.setCondition(req.getCondition() != null ? req.getCondition() : "good");
        fund.setNotes(req.getNotes());
        return toDto(repo.save(fund));
    }

    @Transactional
    public void delete(UUID id, UUID orgId) {
        LibraryFund fund = repo.findByIdAndOrganizationIdAndDeletedFalse(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("LibraryFund", id));
        fund.setDeleted(true);
        fund.setDeletedAt(Instant.now());
        repo.save(fund);
    }

    private LibraryFundDto toDto(LibraryFund f) {
        LibraryFundDto dto = new LibraryFundDto();
        dto.setId(f.getId());
        dto.setOrganizationId(f.getOrganizationId());
        dto.setBranchId(f.getBranchId());
        dto.setAgeGroup(f.getAgeGroup());
        dto.setLanguage(f.getLanguage());
        dto.setPublicationType(f.getPublicationType());
        dto.setTitle(f.getTitle());
        dto.setAuthors(f.getAuthors());
        dto.setPublisher(f.getPublisher());
        dto.setPublishYear(f.getPublishYear());
        dto.setInventoryNumber(f.getInventoryNumber());
        dto.setQuantity(f.getQuantity());
        dto.setReceiptDate(f.getReceiptDate());
        dto.setElectronic(f.isElectronic());
        dto.setOrderNumber(f.getOrderNumber());
        dto.setListItem(f.getListItem());
        dto.setCheckDate(f.getCheckDate());
        dto.setDocLink(f.getDocLink());
        dto.setCondition(f.getCondition());
        dto.setNotes(f.getNotes());
        dto.setCreatedAt(f.getCreatedAt());
        dto.setUpdatedAt(f.getUpdatedAt());
        return dto;
    }
}
