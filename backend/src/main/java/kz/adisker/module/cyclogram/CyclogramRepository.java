package kz.adisker.module.cyclogram;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CyclogramRepository extends JpaRepository<Cyclogram, UUID> {

    List<Cyclogram> findByOrganizationIdAndDeletedFalse(UUID organizationId);

    List<Cyclogram> findByOrganizationIdAndBranchIdAndDeletedFalse(UUID organizationId, UUID branchId);

    List<Cyclogram> findByOrganizationIdAndGroupIdAndDeletedFalse(UUID organizationId, UUID groupId);

    List<Cyclogram> findByOrganizationIdAndGroupIdAndAcademicYearAndDeletedFalse(
            UUID organizationId, UUID groupId, String academicYear);

    Optional<Cyclogram> findByGroupIdAndAcademicYearAndMonthAndWeekAndLanguageAndDeletedFalse(
            UUID groupId, String academicYear, int month, int week, String language);
}
