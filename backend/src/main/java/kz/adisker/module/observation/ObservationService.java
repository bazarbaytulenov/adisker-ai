package kz.adisker.module.observation;

import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.child.Child;
import kz.adisker.module.child.ChildRepository;
import kz.adisker.module.group.Group;
import kz.adisker.module.group.GroupRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ObservationService {

    private final ObservationRepository obsRepo;
    private final ObservationResultRepository resultRepo;
    private final ObservationIndicatorRepository indicatorRepo;
    private final IndividualCardRepository cardRepo;
    private final GroupRepository groupRepo;
    private final ChildRepository childRepo;

    /** Get or create observation for a child */
    @Transactional
    public ObservationDto getOrCreate(UUID orgId, UUID branchId, UUID groupId, UUID childId,
                                      String period, String academicYear, UUID filledBy) {
        Observation obs = obsRepo.findByChildIdAndPeriodAndAcademicYear(childId, period, academicYear)
                .orElseGet(() -> obsRepo.save(Observation.builder()
                        .organizationId(orgId).branchId(branchId).groupId(groupId)
                        .childId(childId).period(period).academicYear(academicYear)
                        .filledBy(filledBy).complete(false).build()));

        List<ObservationResult> results = resultRepo.findByObservationId(obs.getId());

        // Определяем возрастную группу: сначала по Group, потом по birthDate ребёнка
        String ageGroup = resolveAgeGroup(groupId, childId);

        List<ObservationIndicator> indicators;
        if (ageGroup != null) {
            indicators = indicatorRepo.findByOrganizationIdAndAgeGroupAndActiveTrueOrderBySortOrder(orgId, ageGroup);
        } else {
            // fallback: если невозможно определить возраст, вернуть все индикаторы
            indicators = indicatorRepo.findByOrganizationIdAndActiveTrueOrderBySortOrder(orgId);
        }

        ObservationDto dto = new ObservationDto();
        dto.setId(obs.getId()); dto.setChildId(childId);
        dto.setPeriod(period);  dto.setAcademicYear(academicYear);
        dto.setComplete(obs.isComplete());
        dto.setResults(results.stream().map(r -> {
            ResultEntry e = new ResultEntry();
            e.setIndicatorId(r.getIndicatorId()); e.setLevel(r.getLevel()); return e;
        }).collect(Collectors.toList()));
        dto.setIndicators(indicators.stream().map(i -> {
            IndicatorEntry e = new IndicatorEntry();
            e.setId(i.getId()); e.setDomain(i.getDomain());
            e.setCriterion(i.getCriterion()); e.setIndicator(i.getIndicator());
            e.setAgeGroup(i.getAgeGroup()); return e;
        }).collect(Collectors.toList()));
        return dto;
    }

    /** Save a single result level */
    @Transactional
    public void setResult(UUID observationId, UUID indicatorId, String level) {
        resultRepo.deleteByObservationIdAndIndicatorId(observationId, indicatorId);
        if (level != null && !level.isBlank()) {
            resultRepo.save(ObservationResult.builder()
                    .observationId(observationId).indicatorId(indicatorId)
                    .level(level).createdAt(Instant.now()).updatedAt(Instant.now()).build());
        }
    }

    /** Get IKR for child+observation */
    public IndividualCardDto getCard(UUID childId, UUID observationId) {
        var card = cardRepo.findByChildIdAndObservationId(childId, observationId);
        IndividualCardDto dto = new IndividualCardDto();
        card.ifPresent(c -> {
            dto.setId(c.getId()); dto.setChildId(c.getChildId());
            dto.setObservationId(c.getObservationId());
            dto.setGameName(c.getGameName()); dto.setGameObjectives(c.getGameObjectives());
            dto.setGameProcedure(c.getGameProcedure()); dto.setCustomNotes(c.getCustomNotes());
        });
        return dto;
    }

    @Transactional
    public IndividualCardDto saveCard(UUID orgId, UUID childId, UUID observationId,
                                      String gameName, String objectives, String procedure, String notes, String lang) {
        IndividualCard card = cardRepo.findByChildIdAndObservationId(childId, observationId)
                .orElse(IndividualCard.builder().organizationId(orgId)
                        .childId(childId).observationId(observationId).build());
        card.setGameName(gameName); card.setGameObjectives(objectives);
        card.setGameProcedure(procedure); card.setCustomNotes(notes);
        card.setLanguage(lang != null ? lang : "ru");
        cardRepo.save(card);
        return getCard(childId, observationId);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Определяет строку возрастной группы (напр. "3-4", "4-5").
     * 1) Пытается взять из Group.ageFromMonths
     * 2) Если нет — вычисляет по birthDate ребёнка (возраст на 1 сентября текущего учебного года)
     */
    private String resolveAgeGroup(UUID groupId, UUID childId) {
        // Попытка 1: по группе
        Group group = groupRepo.findById(groupId).orElse(null);
        if (group != null && group.getAgeGroup() != null) {
            return group.getAgeGroup();
        }

        // Попытка 2: по дате рождения ребёнка
        Child child = childRepo.findById(childId).orElse(null);
        if (child != null && child.getBirthDate() != null) {
            // Считаем возраст на 1 сентября текущего учебного года
            LocalDate now = LocalDate.now();
            LocalDate septFirst = now.getMonthValue() >= 9
                    ? LocalDate.of(now.getYear(), 9, 1)
                    : LocalDate.of(now.getYear() - 1, 9, 1);
            int months = (int) java.time.temporal.ChronoUnit.MONTHS.between(child.getBirthDate(), septFirst);
            if (months < 36) return "1-3";
            if (months < 48) return "3-4";
            if (months < 60) return "4-5";
            if (months < 72) return "5-6";
            return "6-7";
        }

        return null;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    @Data public static class ObservationDto {
        private UUID id, childId; private String period, academicYear; private boolean complete;
        private List<ResultEntry> results; private List<IndicatorEntry> indicators;
    }
    @Data public static class ResultEntry   { private UUID indicatorId; private String level; }
    @Data public static class IndicatorEntry { private UUID id; private String domain, criterion, indicator, ageGroup, code; }
    @Data public static class IndividualCardDto {
        private UUID id, childId, observationId;
        private String gameName, gameObjectives, gameProcedure, customNotes;
    }
    @Data public static class SetResultRequest { private UUID indicatorId; private String level; }
    @Data public static class SaveCardRequest  {
        private String gameName, gameObjectives, gameProcedure, customNotes, language;
    }
}
