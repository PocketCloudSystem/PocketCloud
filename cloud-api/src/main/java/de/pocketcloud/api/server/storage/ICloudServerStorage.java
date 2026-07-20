package de.pocketcloud.api.server.storage;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.sync.SyncingElement;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ICloudServerStorage extends SyncingElement<Map<String, Object>> {

    UUID serverUuid();

    ICloudServerStorage setAll(Map<String, Object> storage);

    ICloudServerStorage set(String key, Object value);

    ICloudServerStorage remove(String key);

    ICloudServerStorage clear();

    default void push() {
        syncOut();
    }

    boolean has(String key);

    boolean empty();

    Optional<Object> get(String key);

    <T> Optional<T> get(String key, T type);

    int size();

    Optional<ICloudServer> server();

    Map<String, Object> getAll();
}