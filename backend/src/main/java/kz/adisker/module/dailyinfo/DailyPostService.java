package kz.adisker.module.dailyinfo;

import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPostService {

    private final DailyPostRepository repo;

    /** Получить запись за конкретный день (создать если нет) */
    @Transactional
    public DailyPostDto getOrCreate(UUID orgId, UUID branchId, UUID groupId,
                                    LocalDate date, UserPrincipal principal) {
        return repo.findByGroupIdAndPostDateAndDeletedFalse(groupId, date)
                .map(DailyPostDto::from)
                .orElseGet(() -> DailyPostDto.from(repo.save(
                        DailyPost.builder()
                                .organizationId(orgId)
                                .branchId(branchId)
                                .groupId(groupId)
                                .postDate(date)
                                .published(false)
                                .build()
                )));
    }

    /** Список записей за диапазон дат для группы */
    public List<DailyPostDto> list(UUID groupId, LocalDate from, LocalDate to) {
        return repo.findByGroupIdAndPostDateBetweenAndDeletedFalseOrderByPostDateDesc(groupId, from, to)
                .stream().map(DailyPostDto::from).collect(Collectors.toList());
    }

    /** Список записей по дате и филиалу (для директора / методиста) */
    public List<DailyPostDto> listByBranchAndDate(UUID orgId, UUID branchId, LocalDate date) {
        return repo.findByOrganizationIdAndBranchIdAndPostDateAndDeletedFalse(orgId, branchId, date)
                .stream().map(DailyPostDto::from).collect(Collectors.toList());
    }

    /** Сохранить / обновить содержимое */
    @Transactional
    public DailyPostDto save(UUID orgId, UUID id, DailyPostRequest req) {
        DailyPost post = findOwned(id, orgId);
        if (post.isPublished()) throw new BusinessException("Запись опубликована — редактирование запрещено");

        post.setTheme(req.getTheme());
        post.setDescription(req.getDescription());
        post.setHomeTasks(req.getHomeTasks());
        return DailyPostDto.from(repo.save(post));
    }

    /** Опубликовать запись — видна родителям */
    @Transactional
    public DailyPostDto publish(UUID orgId, UUID id) {
        DailyPost post = findOwned(id, orgId);
        if (post.isPublished()) throw new BusinessException("Уже опубликовано");
        post.setPublished(true);
        post.setPublishedAt(Instant.now());
        return DailyPostDto.from(repo.save(post));
    }

    /** Снять с публикации */
    @Transactional
    public DailyPostDto unpublish(UUID orgId, UUID id) {
        DailyPost post = findOwned(id, orgId);
        post.setPublished(false);
        post.setPublishedAt(null);
        return DailyPostDto.from(repo.save(post));
    }

    /** Мягкое удаление */
    @Transactional
    public void delete(UUID orgId, UUID id) {
        DailyPost post = findOwned(id, orgId);
        post.setDeleted(true);
        repo.save(post);
    }

    // ── internal ──────────────────────────────────────────────────────────────
    private DailyPost findOwned(UUID id, UUID orgId) {
        DailyPost post = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DailyPost", id));
        if (!post.getOrganizationId().equals(orgId))
            throw new kz.adisker.common.exception.AccessDeniedException("Нет доступа к этой записи");
        return post;
    }
}
