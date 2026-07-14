package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@NoArgsConstructor
@Getter
public final class CloudSyncServerStoragePacket extends CloudPacket implements CloudboundPacket, AuthenticatedPacket {

    private Map<String, Object> data;

    public CloudSyncServerStoragePacket(Map<String, Object> data) {
        this.data = data != null ? data : Map.of();
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.data = packetData.readMap();
    }

    public static CloudSyncServerStoragePacket create(Map<String, Object> data) {
        return new CloudSyncServerStoragePacket(data);
    }
}
