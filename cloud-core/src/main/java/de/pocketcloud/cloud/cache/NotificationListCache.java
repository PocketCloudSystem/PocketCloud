package de.pocketcloud.cloud.cache;

import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.NotificationListSyncPacket;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NotificationListCache implements LocalCache<String> {

    private final Set<String> notificationList = new HashSet<>();

    @Override
    public void syncIn(List<String> cache) {
        notificationList.clear();
        notificationList.addAll(cache);
    }

    public NotificationListSyncPacket buildSyncPacket() {
        return NotificationListSyncPacket.create(notificationList);
    }

    @Override
    public void syncOut() {
        PacketBroadcaster.broadcast(buildSyncPacket());
    }

    @Override
    public void add(String element) {
        notificationList.add(element);
    }

    @Override
    public void remove(String element) {
        notificationList.remove(element);
    }

    @Override
    public boolean contains(String element) {
        return notificationList.contains(element);
    }

    @Override
    public Collection<String> getAll() {
        return notificationList.stream().toList();
    }
}