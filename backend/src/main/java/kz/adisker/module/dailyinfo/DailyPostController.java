package kz.adisker.module.dailyinfo;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Tag(name = "DailyInfo")
@RestController
@RequestMapping("/daily-posts")
@RequiredArgsConstructor
public class DailyPostController {

    private final DailyPostService service;

    /**
     * GET /daily-posts/today?organizationId=...&branchId=...&groupId=...&date=...
     * Получить (или создать) запись за день для группы.
     * Воспитатель использует для заполнения, родитель — для просмотра.
     */
    @GetMapping("/today")
    public ApiResponse<DailyPostDto> getOrCreate(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam UUID groupId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal UserPrincipal principal) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        return ApiResponse.ok(service.getOrCreate(organizationId, branchId, groupId, targetDate, principal));
    }

    /**
     * GET /daily-posts?groupId=...&from=...&to=...
     * Список записей группы за период.
     */
    @GetMapping
    public ApiResponse<List<DailyPostDto>> list(
            @RequestParam UUID groupId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(service.list(groupId, from, to));
    }

    /**
     * GET /daily-posts/branch?organizationId=...&branchId=...&date=...
     * Все записи по филиалу за дату (для директора / методиста).
     */
    @GetMapping("/branch")
    @PreAuthorize("hasAnyRole('DIRECTOR','METHODIST','SYSTEM_ADMIN','FOUNDER')")
    public ApiResponse<List<DailyPostDto>> listByBranch(
            @RequestParam UUID organizationId,
            @RequestParam UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok(service.listByBranchAndDate(organizationId, branchId, date));
    }

    /**
     * PUT /daily-posts/{id}?organizationId=...
     * Сохранить содержимое.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<DailyPostDto> save(
            @PathVariable UUID id,
            @RequestParam UUID organizationId,
            @Valid @RequestBody DailyPostRequest req) {
        return ApiResponse.ok(service.save(organizationId, id, req));
    }

    /**
     * PATCH /daily-posts/{id}/publish?organizationId=...
     * Опубликовать (видна родителям).
     */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<DailyPostDto> publish(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.publish(organizationId, id));
    }

    /**
     * PATCH /daily-posts/{id}/unpublish?organizationId=...
     * Снять с публикации.
     */
    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<DailyPostDto> unpublish(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        return ApiResponse.ok(service.unpublish(organizationId, id));
    }

    /**
     * DELETE /daily-posts/{id}?organizationId=...
     * Мягкое удаление.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDUCATOR','METHODIST','DIRECTOR','SYSTEM_ADMIN')")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            @RequestParam UUID organizationId) {
        service.delete(organizationId, id);
        return ApiResponse.ok("Удалено", null);
    }
}
