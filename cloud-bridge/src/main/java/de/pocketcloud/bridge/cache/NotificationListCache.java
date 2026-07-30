package de.pocketcloud.bridge.cache;

import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.sync.SyncType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Local notification cache for the server
 */
public final class NotificationListCache implements LocalCache<String, Boolean> {

    private final Map<String, Boolean> notificationList = new HashMap<>();

    @Override
    public void syncIn(Map<String, Boolean> cache) {
        notificationList.clear();
        notificationList.putAll(cache);
    }

    @Override
    public void syncOut() {}

    @Override
    public void add(String key, @NotNull Boolean value) {
        notificationList.put(key, value);
        SyncPacket.create(SyncType.PLAYER_NOTIFICATION_STATE, data -> data.writeAll(key, value));
    }

    @Override
    public void remove(String element) {
        notificationList.remove(element);
        SyncPacket.create(SyncType.PLAYER_NOTIFICATION_STATE, data -> data.writeAll(element, false));
    }

    @Override
    public void clear() {
        notificationList.clear();
        syncOut();
    }

    @Override
    public boolean contains(String element) {
        return notificationList.containsKey(element);
    }

    @Override
    public int size() {
        return notificationList.size();
    }

    @Override
    public Optional<Boolean> get(String id) {
        return Optional.ofNullable(notificationList.get(id));
    }

    @Override
    public Map<String, Boolean> getAll() {
        return Collections.unmodifiableMap(notificationList);
    }
}