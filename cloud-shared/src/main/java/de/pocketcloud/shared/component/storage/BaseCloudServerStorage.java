package de.pocketcloud.shared.component.storage;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.server.storage.ICloudServerStorage;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;

public abstract class BaseCloudServerStorage implements ICloudServerStorage {

    @Getter
    @Accessors(fluent = true)
    protected final UUID serverUuid;
    protected final Map<String, Object> storage = new HashMap<>();

    public BaseCloudServerStorage(UUID serverUuid) {
        this.serverUuid = serverUuid;
    }

    @Override
    public BaseCloudServerStorage setAll(Map<String, Object> storage) {
        this.storage.putAll(storage);
        return this;
    }

    @Override
    public BaseCloudServerStorage set(String key, Object value) {
        storage.put(key, value);
        return this;
    }

    @Override
    public BaseCloudServerStorage remove(String key) {
        storage.remove(key);
        return this;
    }

    @Override
    public boolean has(String key) {
        return storage.containsKey(key);
    }

    @Override
    public Optional<Object> get(String key) {
        return Optional.ofNullable(storage.get(key));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, T type) {
        Object value = get(key).orElse(null);
        if (value == null) return Optional.empty();
        return Optional.of((T) value);
    }

    @Override
    public BaseCloudServerStorage clear() {
        this.storage.clear();
        return this;
    }

    @Override
    public boolean empty() {
        return storage.isEmpty();
    }

    @Override
    public int size() {
        return storage.size();
    }

    @Override
    public Optional<ICloudServer> server() {
        return CloudAPI.instance().servers().get(serverUuid);
    }

    @Override
    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(storage);
    }
}