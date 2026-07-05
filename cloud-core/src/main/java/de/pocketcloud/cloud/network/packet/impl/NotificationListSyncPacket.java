package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor
@Getter
public final class NotificationListSyncPacket extends CloudPacket implements ClientboundPacket {

    private List<String> list;

    public NotificationListSyncPacket(List<String> list) {
        this.list = list != null ? list : List.of();
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(list);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static NotificationListSyncPacket create(List<String> list) {
        return new NotificationListSyncPacket(list);
    }

    public static NotificationListSyncPacket fromNotificationListCache() {
        return new NotificationListSyncPacket(LocalCache.get(NotificationListCache.class).getAll().stream().toList());
    }
}
