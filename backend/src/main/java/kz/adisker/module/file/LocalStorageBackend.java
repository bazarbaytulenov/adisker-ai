package kz.adisker.module.file;

import kz.adisker.common.exception.BusinessException;
import kz.adisker.common.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.*;

/**
 * Локальное файловое хранилище (диск сервера).
 * Активно, когда storage.type=local (по умолчанию).
 */
@Slf4j
@Component
public class LocalStorageBackend implements StorageBackend {

    private final Path root;

    public LocalStorageBackend(@Value("${storage.local.path:/var/adisker/uploads}") String rootPath) {
        this.root = Paths.get(rootPath).toAbsolutePath().normalize();
    }

    @Override
    public String type() {
        return "local";
    }

    @Override
    public String store(String relativeKey, InputStream content, long size, String contentType) {
        try {
            Path target = resolveSafe(relativeKey);
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (Exception e) {
            throw new BusinessException("Не удалось сохранить файл: " + e.getMessage());
        }
    }

    @Override
    public InputStream load(String storagePath) {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            // защита от чтения за пределами корня хранилища
            if (!path.startsWith(root)) {
                throw new ResourceNotFoundException("File", storagePath);
            }
            if (!Files.exists(path)) {
                throw new ResourceNotFoundException("File", storagePath);
            }
            return Files.newInputStream(path);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Не удалось прочитать файл: " + e.getMessage());
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path path = Paths.get(storagePath).toAbsolutePath().normalize();
            if (path.startsWith(root)) {
                Files.deleteIfExists(path);
            }
        } catch (Exception e) {
            log.warn("Не удалось удалить файл {}: {}", storagePath, e.getMessage());
        }
    }

    /** Разрешает ключ относительно корня, не допуская выхода за его пределы (path traversal). */
    private Path resolveSafe(String relativeKey) {
        Path resolved = root.resolve(relativeKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new BusinessException("Недопустимый путь файла");
        }
        return resolved;
    }
}
