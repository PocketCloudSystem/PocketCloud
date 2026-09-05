package de.pocketcloud.cloud.cache;

import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.sync.SyncType;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class NotificationListCache implements LocalCache<String, Boolean>, SyncingElement<Map<String, Boolean>> {

    private final Map<String, Boolean> notificationList = new ConcurrentHashMap<>();

    @Override
    public void syncIn(Map<String, Boolean> cache) {
        notificationList.clear();
        notificationList.putAll(cache);
    }

    public SyncPacket buildSyncPacket() {
        return SyncPacket.create(SyncType.NOTIFICATION_LIST, data -> data.write(notificationList));
    }

    @Override
    public void syncOut() {
        PacketBroadcaster.broadcast(buildSyncPacket());
    }

    @Override
    public void add(String key, @NotNull Boolean value) {
        notificationList.put(key, value);
        syncOut();
    }

    @Override
    public void remove(String element) {
        notificationList.remove(element);
        syncOut();
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