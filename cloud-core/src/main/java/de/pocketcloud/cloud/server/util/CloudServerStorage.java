package de.pocketcloud.cloud.server.util;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.server.storage.ICloudServerStorage;

import java.util.*;

public final class CloudServerStorage implements ICloudServerStorage {

    private final UUID serverUuid;
    private final Map<String, Object> storage = new HashMap<>();

    public CloudServerStorage(UUID serverUuid) {
        this.serverUuid = serverUuid;
    }

    public CloudServerStorage(UUID serverUuid, Map<String, Object> storage) {
        this.serverUuid = serverUuid;
        this.storage.putAll(storage);
    }

    public CloudServerStorage setAll(Map<String, Object> storage) {
        this.storage.putAll(storage);
        return this;
    }

    public CloudServerStorage set(String key, Object value) {
        storage.put(key, value);
        return this;
    }

    public CloudServerStorage remove(String key) {
        storage.remove(key);
        return this;
    }

    public boolean has(String key) {
        return storage.containsKey(key);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(storage.getOrDefault(key, null));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> get(String key, T type) {
        Object value = get(key).orElse(null);
        if (value == null) return Optional.empty();
        return Optional.of((T) value);
    }

    public void clear() {
        this.storage.clear();
    }

    public boolean empty() {
        return storage.isEmpty();
    }

    public int size() {
        return storage.size();
    }

    public UUID serverUuid() {
        return serverUuid;
    }

    public Optional<? extends ICloudServer> server() {
        return CloudAPI.instance().servers().get(serverUuid);
    }

    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(storage);
    }
}