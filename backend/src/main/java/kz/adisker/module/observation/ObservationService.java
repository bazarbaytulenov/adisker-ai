package kz.adisker.module.observation;

import kz.adisker.common.exception.ResourceNotFoundException;
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
public class ObservationService {

    private final ObservationRepository obsRepo;
    private final ObservationResultRepository resultRepo;
    private final ObservationIndicatorRepository indicatorRepo;
    private final IndividualCardRepository cardRepo;

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
        List<ObservationIndicator> indicators = indicatorRepo
                .findByOrganizationIdAndActiveTrueOrderBySortOrder(orgId);

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

    // ── DTOs ─────────────────────────────────────────────────────────────────
    @Data public static class ObservationDto {
        private UUID id, childId; private String period, academicYear; private boolean complete;
        private List<ResultEntry> results; private List<IndicatorEntry> indicators;
    }
    @Data public static class ResultEntry   { private UUID indicatorId; private String level; }
    @Data public static class IndicatorEntry { private UUID id; private String domain, criterion, indicator, ageGroup; }
    @Data public static class IndividualCardDto {
        private UUID id, childId, observationId;
        private String gameName, gameObjectives, gameProcedure, customNotes;
    }
    @Data public static class SetResultRequest { private UUID indicatorId; private String level; }
    @Data public static class SaveCardRequest  {
        private String gameName, gameObjectives, gameProcedure, customNotes, language;
    }
}
