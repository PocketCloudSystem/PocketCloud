package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@NoArgsConstructor
@Getter
public final class MaintenanceListSyncPacket extends CloudPacket implements ClientboundPacket {

    private Collection<String> list;

    public MaintenanceListSyncPacket(Collection<String> list) {
        this.list = list != null ? list : List.of();
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(list);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static MaintenanceListSyncPacket create(Collection<String> list) {
        return new MaintenanceListSyncPacket(list);
    }
}
