package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.type.NotificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Getter
public final class CloudNotificationPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private NotificationType notificationType;
    private Map<String, Object> args;

    public CloudNotificationPacket(NotificationType notificationType, Map<String, Object> args) {
        this.notificationType = notificationType;
        this.args = args != null ? args : Map.of();
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(notificationType, args);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.notificationType = packetData.readEnum(NotificationType.class);
        this.args = packetData.readMap();
    }

    public static CloudNotificationPacket create(NotificationType notificationType, Map<String, Object> args) {
        return new CloudNotificationPacket(notificationType, args);
    }
}
