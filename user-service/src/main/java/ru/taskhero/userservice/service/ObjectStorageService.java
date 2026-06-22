package ru.taskhero.userservice.service;

/**
 * Тонкая обёртка над S3-совместимым объектным хранилищем.
 */
public interface ObjectStorageService {

    /**
     * Загрузить файл в хранилище.
     *
     * @param key         ключ объекта в бакете, например {@code avatars/<uuid>.png}
     * @param content     содержимое файла
     * @param contentType MIME-тип файла
     * @return публичный URL загруженного файла
     */
    String upload(String key, byte[] content, String contentType);

    /**
     * Удалить файл из хранилища.
     *
     * @param key ключ объекта в бакете
     */
    void delete(String key);
}
