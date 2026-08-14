package kz.adisker.module.attendance;

import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.child.ChildRepository;
import kz.adisker.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceService {

    private final AttendanceMonthRepository monthRepo;
    private final AttendanceMarkRepository markRepo;
    private final ChildRepository childRepo;

    /** Get or create a month sheet for a group */
    @Transactional
    public AttendanceSheetDto getOrCreateSheet(UUID orgId, UUID branchId, UUID groupId, int year, int month) {
        AttendanceMonth am = monthRepo.findByGroupIdAndYearAndMonth(groupId, year, month)
                .orElseGet(() -> monthRepo.save(AttendanceMonth.builder()
                        .organizationId(orgId).branchId(branchId).groupId(groupId)
                        .year(year).month(month).closed(false).createdAt(Instant.now()).build()));

        List<AttendanceMark> marks = markRepo.findByAttendanceMonthId(am.getId());

        // Build map: childId -> { day -> mark }
        Map<UUID, Map<Integer, String>> byChild = new LinkedHashMap<>();
        for (AttendanceMark m : marks) {
            byChild.computeIfAbsent(m.getChildId(), k -> new TreeMap<>()).put(m.getDay(), m.getMark());
        }

        // Load active children for the group
        var children = childRepo.findByOrganizationIdAndGroupIdAndDeletedFalse(orgId, groupId,
                org.springframework.data.domain.Pageable.unpaged());

        List<ChildAttendanceRow> rows = children.getContent().stream().map(c -> {
            ChildAttendanceRow row = new ChildAttendanceRow();
            row.setChildId(c.getId());
            row.setFullName(c.getLastName() + " " + c.getFirstName());
            row.setMarks(byChild.getOrDefault(c.getId(), Collections.emptyMap()));
            return row;
        }).collect(Collectors.toList());

        AttendanceSheetDto dto = new AttendanceSheetDto();
        dto.setMonthId(am.getId());
        dto.setGroupId(groupId);
        dto.setYear(year);
        dto.setMonth(month);
        dto.setClosed(am.isClosed());
        dto.setRows(rows);
        return dto;
    }

    /** Set a single mark */
    @Transactional
    public void setMark(UUID monthId, UUID childId, UUID orgId, int day, String mark, UserPrincipal principal) {
        AttendanceMonth am = monthRepo.findById(monthId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceMonth", monthId));
        if (am.isClosed()) throw new kz.adisker.common.exception.BusinessException("Табель закрыт");

        markRepo.deleteByMonthAndChildAndDay(monthId, childId, day);
        if (mark != null && !mark.isBlank()) {
            markRepo.save(AttendanceMark.builder()
                    .attendanceMonthId(monthId).childId(childId).organizationId(orgId)
                    .day(day).mark(mark).createdAt(Instant.now()).updatedAt(Instant.now())
                    .updatedBy(principal.getId()).build());
        }
    }

    /** Close (lock) the month sheet */
    @Transactional
    public void closeMonth(UUID monthId, UserPrincipal principal) {
        AttendanceMonth am = monthRepo.findById(monthId)
                .orElseThrow(() -> new ResourceNotFoundException("AttendanceMonth", monthId));
        am.setClosed(true);
        am.setClosedBy(principal.getId());
        am.setClosedAt(Instant.now());
        monthRepo.save(am);
    }

    // ── DTOs ───────────────────────────────────────────────────────────────
    @Data public static class AttendanceSheetDto {
        private UUID monthId;
        private UUID groupId;
        private int year, month;
        private boolean closed;
        private List<ChildAttendanceRow> rows;
    }

    @Data public static class ChildAttendanceRow {
        private UUID childId;
        private String fullName;
        private Map<Integer, String> marks; // day -> mark
    }

    @Data public static class MarkRequest {
        private UUID childId;
        private int day;
        private String mark;
    }
}
