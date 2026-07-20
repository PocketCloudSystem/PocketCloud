package de.pocketcloud.bridge.notification;

import de.pocketcloud.network.packet.impl.CloudNotificationPacket;
import de.pocketcloud.shared.network.packet.type.NotificationType;

import java.util.Map;

public final class NotificationService {

    public void sendNotification(NotificationType type, Map<String, Object> args) {
        CloudNotificationPacket.create(type, args).sendPacket();
    }
}