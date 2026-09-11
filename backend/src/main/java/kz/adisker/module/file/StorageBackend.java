package kz.adisker.module.file;

import java.io.InputStream;

/**
 * Абстракция файлового хранилища. Реализации: local (диск), s3 (позже).
 * Выбор реализации управляется свойством storage.type.
 */
public interface StorageBackend {

    /** Тип хранилища: "local" / "s3". */
    String type();

    /**
     * Сохранить контент под относительным ключом (например, "org/{id}/daily_post/{uuid}.jpg").
     * @return абсолютный/канонический путь или ключ для последующего чтения.
     */
    String store(String relativeKey, InputStream content, long size, String contentType);

    /** Открыть поток на чтение по ранее сохранённому пути/ключу. */
    InputStream load(String storagePath);

    /** Удалить объект. Отсутствие объекта не считается ошибкой. */
    void delete(String storagePath);
}
