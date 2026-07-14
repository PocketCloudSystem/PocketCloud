package de.pocketcloud.api.server.storage;

import de.pocketcloud.api.model.server.ICloudServer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ICloudServerStorage {

    UUID serverUuid();

    ICloudServerStorage setAll(Map<String, Object> storage);

    ICloudServerStorage set(String key, Object value);

    ICloudServerStorage remove(String key);

    void clear();

    boolean has(String key);

    boolean empty();

    Optional<Object> get(String key);

    <T> Optional<T> get(String key, T type);

    int size();

    Optional<? extends ICloudServer> server();

    Map<String, Object> getAll();
}