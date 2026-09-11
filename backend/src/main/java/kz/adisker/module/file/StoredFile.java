package kz.adisker.module.file;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Метаданные загруженного файла. Соответствует таблице files (V4).
 * Сам контент хранится в файловом хранилище (local / s3).
 */
@Entity
@Table(name = "files")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StoredFile {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @Column(name = "organization_id", nullable = false) private UUID organizationId;
    @Column(name = "uploaded_by",     nullable = false) private UUID uploadedBy;

    @Column(name = "file_name",     nullable = false) private String fileName;      // имя в хранилище
    @Column(name = "original_name", nullable = false) private String originalName;  // исходное имя
    @Column(name = "mime_type")  private String mimeType;
    @Column(name = "size_bytes") private Long sizeBytes;

    @Column(name = "storage_path", nullable = false) private String storagePath;
    @Column(name = "storage_type", nullable = false) private String storageType = "local";

    @Column(name = "entity_type") private String entityType; // daily_post / child_photo / chat / ...
    @Column(name = "entity_id")   private UUID entityId;

    @Column(name = "is_public", nullable = false) private boolean isPublic = false;

    @Column(name = "created_at", nullable = false) private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
