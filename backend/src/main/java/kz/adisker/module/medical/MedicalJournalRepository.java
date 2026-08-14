package kz.adisker.module.medical;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MedicalJournalRepository extends JpaRepository<MedicalJournal, UUID> {

    Page<MedicalJournal> findByOrganizationIdAndBranchIdOrderByJournalDateDesc(
            UUID orgId, UUID branchId, Pageable pageable);

    List<MedicalJournal> findByOrganizationIdAndBranchIdAndJournalTypeAndJournalDateBetweenOrderByJournalDateAsc(
            UUID orgId, UUID branchId, String journalType, LocalDate from, LocalDate to);

    Optional<MedicalJournal> findByOrganizationIdAndBranchIdAndJournalTypeAndJournalDate(
            UUID orgId, UUID branchId, String journalType, LocalDate journalDate);

    Optional<MedicalJournal> findByIdAndOrganizationId(UUID id, UUID orgId);
}
