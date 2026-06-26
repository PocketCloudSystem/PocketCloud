package de.pocketcloud.cloud.server.util;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.CloudServerManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class CloudServerStorage {

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

    public @Nullable Object get(String key) {
        return storage.getOrDefault(key, null);
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

    public Optional<CloudServer> server() {
        return CloudServerManager.instance().get(serverUuid);
    }

    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(storage);
    }
}