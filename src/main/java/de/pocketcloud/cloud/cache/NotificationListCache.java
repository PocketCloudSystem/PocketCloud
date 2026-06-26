package de.pocketcloud.cloud.cache;

import de.pocketcloud.cloud.network.packet.impl.NotificationListSyncPacket;
import de.pocketcloud.cloud.template.TemplateType;

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

    @Override
    public void syncOut() {
        NotificationListSyncPacket.fromNotificationListCache().broadcastPacket();
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