package kz.adisker.module.file;

import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import kz.adisker.module.audit.AuditService;
import kz.adisker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Загрузка, чтение и удаление файлов (ТЗ 5.16, 5.6, п.10, п.11).
 * Ограничения: фото ≤ 5 МБ (image/*), документы ≤ 20 МБ.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {

    private final StoredFileRepository repo;
    private final List<StorageBackend> backends;
    private final AuditService auditService;

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${storage.max-photo-size:5242880}")   // 5 MB
    private long maxPhotoSize;

    @Value("${storage.max-doc-size:20971520}")     // 20 MB
    private long maxDocSize;

    @Transactional
    public StoredFile upload(MultipartFile file, String entityType, UUID entityId,
                             boolean isPublic, UserPrincipal principal) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Файл пуст");
        }

        String contentType = file.getContentType();
        long size = file.getSize();
        boolean isImage = contentType != null && contentType.startsWith("image/");

        // Ограничения размера по типу
        long limit = isImage ? maxPhotoSize : maxDocSize;
        if (size > limit) {
            throw new BusinessException(String.format(
                    "Файл превышает допустимый размер (%d МБ)", limit / (1024 * 1024)));
        }

        StorageBackend backend = selectBackend();
        String ext = extensionOf(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + ext;
        String relativeKey = String.format("org/%s/%s/%s",
                principal.getOrganizationId(),
                entityType != null ? entityType : "misc",
                storedName);

        String storagePath;
        try {
            storagePath = backend.store(relativeKey, file.getInputStream(), size, contentType);
        } catch (IOException e) {
            throw new BusinessException("Ошибка чтения загружаемого файла");
        }

        StoredFile meta = repo.save(StoredFile.builder()
                .organizationId(principal.getOrganizationId())
                .uploadedBy(principal.getId())
                .fileName(storedName)
                .originalName(file.getOriginalFilename() != null ? file.getOriginalFilename() : storedName)
                .mimeType(contentType)
                .sizeBytes(size)
                .storagePath(storagePath)
                .storageType(backend.type())
                .entityType(entityType)
                .entityId(entityId)
                .isPublic(isPublic)
                .build());

        auditService.record("CREATE", "file", meta.getId(), null, null,
                "Загружен файл: " + meta.getOriginalName());
        return meta;
    }

    public StoredFile getMeta(UUID id, UUID orgId) {
        return repo.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("File", id));
    }

    public java.io.InputStream openStream(StoredFile meta) {
        return selectBackend(meta.getStorageType()).load(meta.getStoragePath());
    }

    public List<StoredFile> listForEntity(String entityType, UUID entityId) {
        return repo.findByEntityTypeAndEntityIdOrderByCreatedAt(entityType, entityId);
    }

    public long countForEntity(String entityType, UUID entityId) {
        return repo.countByEntityTypeAndEntityId(entityType, entityId);
    }

    @Transactional
    public void delete(UUID id, UserPrincipal principal) {
        StoredFile meta = getMeta(id, principal.getOrganizationId());
        selectBackend(meta.getStorageType()).delete(meta.getStoragePath());
        repo.delete(meta);
        auditService.record("DELETE", "file", id, null, null,
                "Удалён файл: " + meta.getOriginalName());
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private StorageBackend selectBackend() {
        return selectBackend(storageType);
    }

    private StorageBackend selectBackend(String type) {
        return backends.stream()
                .filter(b -> b.type().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "Тип хранилища не поддерживается: " + type));
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        String ext = filename.substring(dot);
        // допускаем только безопасные расширения (буквы/цифры)
        return ext.matches("\\.[A-Za-z0-9]{1,10}") ? ext.toLowerCase() : "";
    }
}
