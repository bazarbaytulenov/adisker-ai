package kz.adisker.module.medical;

import kz.adisker.common.dto.PageResponse;
import kz.adisker.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicalJournalService {

    private final MedicalJournalRepository repo;

    public PageResponse<MedicalJournalDto> list(UUID orgId, UUID branchId, Pageable pageable) {
        var page = repo.findByOrganizationIdAndBranchIdOrderByJournalDateDesc(orgId, branchId, pageable);
        return PageResponse.from(page.map(this::toDto));
    }

    public List<MedicalJournalDto> listByType(UUID orgId, UUID branchId, String journalType,
                                               LocalDate from, LocalDate to) {
        return repo.findByOrganizationIdAndBranchIdAndJournalTypeAndJournalDateBetweenOrderByJournalDateAsc(
                orgId, branchId, journalType, from, to)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Get or create a journal entry for a given type and date.
     */
    @Transactional
    public MedicalJournalDto getOrCreate(UUID orgId, UUID branchId, String journalType, LocalDate date) {
        return toDto(repo.findByOrganizationIdAndBranchIdAndJournalTypeAndJournalDate(
                orgId, branchId, journalType, date)
                .orElseGet(() -> repo.save(MedicalJournal.builder()
                        .organizationId(orgId)
                        .branchId(branchId)
                        .journalType(journalType)
                        .journalDate(date)
                        .data("{}")
                        .build())));
    }

    public MedicalJournalDto getById(UUID id, UUID orgId) {
        return toDto(repo.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalJournal", id)));
    }

    @Transactional
    public MedicalJournalDto create(UUID orgId, MedicalJournalRequest req) {
        MedicalJournal journal = MedicalJournal.builder()
                .organizationId(orgId)
                .branchId(req.getBranchId())
                .journalType(req.getJournalType())
                .journalDate(req.getJournalDate())
                .data(req.getData() != null ? req.getData() : "{}")
                .notes(req.getNotes())
                .build();
        return toDto(repo.save(journal));
    }

    @Transactional
    public MedicalJournalDto update(UUID id, UUID orgId, MedicalJournalRequest req) {
        MedicalJournal journal = repo.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalJournal", id));
        journal.setJournalType(req.getJournalType());
        journal.setJournalDate(req.getJournalDate());
        journal.setData(req.getData() != null ? req.getData() : "{}");
        journal.setNotes(req.getNotes());
        return toDto(repo.save(journal));
    }

    @Transactional
    public MedicalJournalDto saveData(UUID id, UUID orgId, String jsonData) {
        MedicalJournal journal = repo.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalJournal", id));
        journal.setData(jsonData);
        return toDto(repo.save(journal));
    }

    @Transactional
    public void delete(UUID id, UUID orgId) {
        MedicalJournal journal = repo.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalJournal", id));
        repo.delete(journal);
    }

    private MedicalJournalDto toDto(MedicalJournal j) {
        MedicalJournalDto dto = new MedicalJournalDto();
        dto.setId(j.getId());
        dto.setOrganizationId(j.getOrganizationId());
        dto.setBranchId(j.getBranchId());
        dto.setJournalType(j.getJournalType());
        dto.setJournalDate(j.getJournalDate());
        dto.setData(j.getData());
        dto.setNotes(j.getNotes());
        dto.setCreatedAt(j.getCreatedAt());
        dto.setUpdatedAt(j.getUpdatedAt());
        return dto;
    }
}
