package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.cloud.cache.WhitelistCache;
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
public final class MaintenanceListSyncPacket extends CloudPacket implements ClientboundPacket {

    private List<String> list;

    public MaintenanceListSyncPacket(List<String> list) {
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

    public static MaintenanceListSyncPacket create(List<String> list) {
        return new MaintenanceListSyncPacket(list);
    }

    public static MaintenanceListSyncPacket fromMaintenanceListCache() {
        return new MaintenanceListSyncPacket(LocalCache.get(WhitelistCache.class).getAll().stream().toList());
    }
}
