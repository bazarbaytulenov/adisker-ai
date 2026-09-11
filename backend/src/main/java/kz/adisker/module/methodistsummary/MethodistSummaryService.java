package kz.adisker.module.methodistsummary;

import kz.adisker.module.observation.*;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MethodistSummaryService {

    private final MethodistSummaryRepository summaryRepo;
    private final ObservationRepository      obsRepo;
    private final ObservationResultRepository resultRepo;
    private final ObservationIndicatorRepository indicatorRepo;

    // ── Получить свод (из кэша или вычислить на лету) ─────────────────────
    public SummaryResponseDto getSummary(UUID orgId, UUID branchId, UUID groupId,
                                         String period, String academicYear) {
        List<MethodistSummary> cached = loadCached(orgId, branchId, groupId, period, academicYear);
        if (!cached.isEmpty()) {
            return toDto(cached, period, academicYear);
        }
        // Нет кэша — вычисляем на лету (но не сохраняем)
        return compute(orgId, branchId, groupId, period, academicYear);
    }

    // ── Пересчитать и сохранить ───────────────────────────────────────────
    @Transactional
    public SummaryResponseDto recalculate(UUID orgId, UUID branchId, UUID groupId,
                                          String period, String academicYear) {
        // Удаляем старые записи
        if (groupId != null) {
            summaryRepo.deleteByOrganizationIdAndBranchIdAndGroupIdAndPeriodAndAcademicYear(
                    orgId, branchId, groupId, period, academicYear);
        } else {
            summaryRepo.deleteByOrganizationIdAndBranchIdAndGroupIdIsNullAndPeriodAndAcademicYear(
                    orgId, branchId, period, academicYear);
        }

        SummaryResponseDto dto = compute(orgId, branchId, groupId, period, academicYear);

        // Сохраняем строки
        Instant now = Instant.now();
        List<MethodistSummary> rows = dto.getRows().stream().map(r -> MethodistSummary.builder()
                .organizationId(orgId).branchId(branchId).groupId(groupId)
                .period(period).academicYear(academicYear)
                .domain(r.getDomain()).ageGroup(r.getAgeGroup())
                .totalChildren(r.getTotalChildren())
                .highCount(r.getHighCount()).midCount(r.getMidCount()).lowCount(r.getLowCount())
                .highPct(r.getHighPct()).midPct(r.getMidPct()).lowPct(r.getLowPct())
                .calculatedAt(now).createdAt(now).updatedAt(now)
                .build()).collect(Collectors.toList());
        summaryRepo.saveAll(rows);
        dto.setCached(true);
        return dto;
    }

    // ── Основная логика вычисления ────────────────────────────────────────
    private SummaryResponseDto compute(UUID orgId, UUID branchId, UUID groupId,
                                       String period, String academicYear) {
        // 1. Все наблюдения по фильтру
        List<Observation> observations;
        if (groupId != null) {
            observations = obsRepo.findByGroupIdAndAcademicYearAndPeriodAndDeletedFalse(
                    groupId, academicYear, period);
        } else {
            // По всем группам филиала — нужен запрос по branchId
            observations = obsRepo.findByBranchIdAndAcademicYearAndPeriodAndDeletedFalse(
                    branchId, academicYear, period);
        }

        if (observations.isEmpty()) {
            return SummaryResponseDto.builder()
                    .period(period).academicYear(academicYear)
                    .rows(Collections.emptyList()).cached(false)
                    .totalObservations(0)
                    .build();
        }

        // 2. Все результаты по этим наблюдениям
        Set<UUID> obsIds = observations.stream().map(Observation::getId).collect(Collectors.toSet());
        List<ObservationResult> allResults = obsIds.stream()
                .flatMap(id -> resultRepo.findByObservationId(id).stream())
                .collect(Collectors.toList());

        // 3. Карта indicatorId → result
        Map<UUID, List<String>> levelsByIndicator = new HashMap<>();
        for (ObservationResult r : allResults) {
            levelsByIndicator.computeIfAbsent(r.getIndicatorId(), k -> new ArrayList<>())
                    .add(r.getLevel());
        }

        // 4. Индикаторы организации
        List<ObservationIndicator> indicators =
                indicatorRepo.findByOrganizationIdAndActiveTrueOrderBySortOrder(orgId);

        // 5. Группировка по domain + ageGroup
        // Структура: domain -> ageGroup -> {V/C/H counts}
        Map<String, Map<String, int[]>> domainAgeMap = new LinkedHashMap<>();

        for (ObservationIndicator ind : indicators) {
            List<String> levels = levelsByIndicator.getOrDefault(ind.getId(), Collections.emptyList());
            if (levels.isEmpty()) continue;

            domainAgeMap
                .computeIfAbsent(ind.getDomain(), d -> new LinkedHashMap<>())
                .computeIfAbsent(ind.getAgeGroup(), a -> new int[3]); // [V, S, H]

            int[] counts = domainAgeMap.get(ind.getDomain()).get(ind.getAgeGroup());
            for (String lvl : levels) {
                if ("V".equals(lvl))      counts[0]++;
                else if ("S".equals(lvl)) counts[1]++;
                else if ("H".equals(lvl)) counts[2]++;
            }
        }

        // 6. Собираем строки DTO
        List<SummaryRowDto> rows = new ArrayList<>();
        for (Map.Entry<String, Map<String, int[]>> domEntry : domainAgeMap.entrySet()) {
            String domain = domEntry.getKey();
            for (Map.Entry<String, int[]> ageEntry : domEntry.getValue().entrySet()) {
                String ageGroup = ageEntry.getKey();
                int[] c = ageEntry.getValue();
                int total = c[0] + c[1] + c[2];
                if (total == 0) continue;

                // totalChildren = число детей у которых есть хоть одна оценка по данному domain
                int childrenWithData = (int) observations.stream()
                        .filter(o -> hasResultsForDomain(o.getId(), domain, indicators, levelsByIndicator))
                        .count();

                rows.add(SummaryRowDto.builder()
                        .domain(domain).ageGroup(ageGroup)
                        .totalChildren(childrenWithData)
                        .highCount(c[0]).midCount(c[1]).lowCount(c[2])
                        .highPct(pct(c[0], total)).midPct(pct(c[1], total)).lowPct(pct(c[2], total))
                        .build());
            }
        }

        return SummaryResponseDto.builder()
                .period(period).academicYear(academicYear)
                .rows(rows).cached(false)
                .totalObservations(observations.size())
                .build();
    }

    private boolean hasResultsForDomain(UUID obsId, String domain,
                                        List<ObservationIndicator> indicators,
                                        Map<UUID, List<String>> levelsByIndicator) {
        return indicators.stream()
                .filter(i -> domain.equals(i.getDomain()))
                .anyMatch(i -> levelsByIndicator.containsKey(i.getId())
                        && resultRepo.findByObservationId(obsId).stream()
                                .anyMatch(r -> r.getIndicatorId().equals(i.getId())));
    }

    private BigDecimal pct(int count, int total) {
        if (total == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(count * 100.0 / total).setScale(1, RoundingMode.HALF_UP);
    }

    private List<MethodistSummary> loadCached(UUID orgId, UUID branchId, UUID groupId,
                                              String period, String academicYear) {
        if (groupId != null) {
            return summaryRepo.findByOrganizationIdAndBranchIdAndGroupIdAndPeriodAndAcademicYear(
                    orgId, branchId, groupId, period, academicYear);
        }
        return summaryRepo.findByOrganizationIdAndBranchIdAndGroupIdIsNullAndPeriodAndAcademicYear(
                orgId, branchId, period, academicYear);
    }

    private SummaryResponseDto toDto(List<MethodistSummary> list, String period, String academicYear) {
        List<SummaryRowDto> rows = list.stream().map(s -> SummaryRowDto.builder()
                .domain(s.getDomain()).ageGroup(s.getAgeGroup())
                .totalChildren(s.getTotalChildren())
                .highCount(s.getHighCount()).midCount(s.getMidCount()).lowCount(s.getLowCount())
                .highPct(s.getHighPct()).midPct(s.getMidPct()).lowPct(s.getLowPct())
                .build()).collect(Collectors.toList());
        return SummaryResponseDto.builder()
                .period(period).academicYear(academicYear)
                .rows(rows).cached(true)
                .totalObservations(0)
                .build();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    @Data @Builder
    public static class SummaryResponseDto {
        private String period;
        private String academicYear;
        private boolean cached;
        private int totalObservations;
        private List<SummaryRowDto> rows;
    }

    @Data @Builder
    public static class SummaryRowDto {
        private String domain;
        private String ageGroup;
        private int totalChildren;
        private int highCount;
        private int midCount;
        private int lowCount;
        private BigDecimal highPct;
        private BigDecimal midPct;
        private BigDecimal lowPct;
    }
}
