package kz.adisker.module.methodistsummary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MethodistSummaryRepository extends JpaRepository<MethodistSummary, UUID> {

    List<MethodistSummary> findByOrganizationIdAndBranchIdAndGroupIdAndPeriodAndAcademicYear(
            UUID orgId, UUID branchId, UUID groupId, String period, String academicYear);

    List<MethodistSummary> findByOrganizationIdAndBranchIdAndGroupIdIsNullAndPeriodAndAcademicYear(
            UUID orgId, UUID branchId, String period, String academicYear);

    void deleteByOrganizationIdAndBranchIdAndGroupIdAndPeriodAndAcademicYear(
            UUID orgId, UUID branchId, UUID groupId, String period, String academicYear);

    void deleteByOrganizationIdAndBranchIdAndGroupIdIsNullAndPeriodAndAcademicYear(
            UUID orgId, UUID branchId, String period, String academicYear);
}
