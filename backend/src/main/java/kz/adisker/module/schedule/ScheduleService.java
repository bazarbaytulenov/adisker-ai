package kz.adisker.module.schedule;

import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.security.UserPrincipal;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository schedRepo;
    private final ScheduleEntryRepository entryRepo;

    /** Получить или создать расписание группы */
    @Transactional
    public ScheduleDto getOrCreate(UUID orgId, UUID branchId, UUID groupId,
                                   String academicYear, String language) {
        Schedule s = schedRepo
                .findByGroupIdAndAcademicYearAndLanguageAndDeletedFalse(groupId, academicYear, language)
                .orElseGet(() -> schedRepo.save(Schedule.builder()
                        .organizationId(orgId).branchId(branchId).groupId(groupId)
                        .academicYear(academicYear).language(language).build()));
        return toDto(s, entryRepo.findByScheduleIdOrderByDayOfWeekAscStartTimeAsc(s.getId()));
    }

    /** Сохранить (полная замена) все записи расписания */
    @Transactional
    public ScheduleDto saveEntries(UUID scheduleId, UUID orgId, List<EntryRequest> entries,
                                   UserPrincipal principal) {
        Schedule s = findAndCheck(scheduleId, orgId);
        entryRepo.deleteByScheduleId(scheduleId);
        List<ScheduleEntry> saved = entries.stream().map(r ->
                entryRepo.save(ScheduleEntry.builder()
                        .scheduleId(scheduleId)
                        .dayOfWeek(r.getDayOfWeek())
                        .startTime(LocalTime.parse(r.getStartTime()))
                        .endTime(r.getEndTime() != null ? LocalTime.parse(r.getEndTime()) : null)
                        .subject(r.getSubject())
                        .educatorId(r.getEducatorId())
                        .educatorRole(r.getEducatorRole())
                        .notes(r.getNotes())
                        .build())
        ).collect(Collectors.toList());
        return toDto(s, saved);
    }

    /** Опубликовать расписание */
    @Transactional
    public ScheduleDto publish(UUID scheduleId, UUID orgId, UserPrincipal principal) {
        Schedule s = findAndCheck(scheduleId, orgId);
        s.setPublished(true);
        s.setPublishedAt(Instant.now());
        return toDto(schedRepo.save(s),
                entryRepo.findByScheduleIdOrderByDayOfWeekAscStartTimeAsc(scheduleId));
    }

    private Schedule findAndCheck(UUID id, UUID orgId) {
        Schedule s = schedRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", id));
        if (!s.getOrganizationId().equals(orgId))
            throw new kz.adisker.common.exception.AccessDeniedException("Нет доступа");
        return s;
    }

    private ScheduleDto toDto(Schedule s, List<ScheduleEntry> entries) {
        return ScheduleDto.builder()
                .id(s.getId()).organizationId(s.getOrganizationId())
                .branchId(s.getBranchId()).groupId(s.getGroupId())
                .academicYear(s.getAcademicYear()).language(s.getLanguage())
                .approvalInfo(s.getApprovalInfo()).published(s.isPublished())
                .entries(entries.stream().map(e -> EntryDto.builder()
                        .id(e.getId()).dayOfWeek(e.getDayOfWeek())
                        .startTime(e.getStartTime().toString())
                        .endTime(e.getEndTime() != null ? e.getEndTime().toString() : null)
                        .subject(e.getSubject()).educatorId(e.getEducatorId())
                        .educatorRole(e.getEducatorRole()).notes(e.getNotes())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @Data @lombok.Builder
    public static class ScheduleDto {
        private UUID id, organizationId, branchId, groupId;
        private String academicYear, language, approvalInfo;
        private boolean published;
        private List<EntryDto> entries;
    }

    @Data @lombok.Builder
    public static class EntryDto {
        private UUID id, educatorId;
        private int dayOfWeek;
        private String startTime, endTime, subject, educatorRole, notes;
    }

    @Data
    public static class EntryRequest {
        private int dayOfWeek;
        private String startTime, endTime, subject, educatorRole, notes;
        private UUID educatorId;
    }
}
