package kz.adisker.module.file;

import io.swagger.v3.oas.annotations.tags.Tag;
import kz.adisker.common.dto.ApiResponse;
import kz.adisker.security.UserPrincipal;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Tag(name = "Files")
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService service;

    /** POST /api/files (multipart) — загрузка файла с привязкой к сущности. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<FileDto> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(defaultValue = "false") boolean isPublic,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok("Файл загружен",
                FileDto.from(service.upload(file, entityType, entityId, isPublic, principal)));
    }

    /** GET /api/files/{id} — метаданные файла. */
    @GetMapping("/{id}")
    public ApiResponse<FileDto> meta(@PathVariable UUID id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(FileDto.from(service.getMeta(id, principal.getOrganizationId())));
    }

    /** GET /api/files/{id}/content — скачивание содержимого. */
    @GetMapping("/{id}/content")
    public ResponseEntity<InputStreamResource> download(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        StoredFile meta = service.getMeta(id, principal.getOrganizationId());
        InputStreamResource body = new InputStreamResource(service.openStream(meta));
        MediaType mediaType = meta.getMimeType() != null
                ? MediaType.parseMediaType(meta.getMimeType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + meta.getOriginalName() + "\"")
                .contentType(mediaType)
                .body(body);
    }

    /** GET /api/files?entityType=&entityId= — файлы, привязанные к сущности. */
    @GetMapping
    public ApiResponse<List<FileDto>> listForEntity(
            @RequestParam String entityType,
            @RequestParam UUID entityId) {
        return ApiResponse.ok(service.listForEntity(entityType, entityId)
                .stream().map(FileDto::from).toList());
    }

    /** DELETE /api/files/{id} */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        service.delete(id, principal);
        return ApiResponse.ok("Удалено", null);
    }

    // ── DTO ──────────────────────────────────────────────────────────────────
    @Data @Builder
    public static class FileDto {
        private UUID id;
        private String originalName;
        private String mimeType;
        private Long sizeBytes;
        private String entityType;
        private UUID entityId;
        private boolean isPublic;
        private Instant createdAt;
        private String url; // относительный путь для скачивания

        static FileDto from(StoredFile f) {
            return FileDto.builder()
                    .id(f.getId())
                    .originalName(f.getOriginalName())
                    .mimeType(f.getMimeType())
                    .sizeBytes(f.getSizeBytes())
                    .entityType(f.getEntityType())
                    .entityId(f.getEntityId())
                    .isPublic(f.isPublic())
                    .createdAt(f.getCreatedAt())
                    .url("/api/files/" + f.getId() + "/content")
                    .build();
        }
    }
}
