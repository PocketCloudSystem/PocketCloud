package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@NoArgsConstructor
@Getter
public final class CloudSyncServerStoragePacket extends CloudPacket implements CloudboundPacket {

    private Map<String, Object> data;

    public CloudSyncServerStoragePacket(Map<String, Object> data) {
        this.data = data != null ? data : Map.of();
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var server = client.server();
        if (server != null) {
            server.storage().clear();
            server.storage().setAll(data);
        }
    }

    @Override
    public void encodePayload(PacketData packetData) {}

    @Override
    public void decodePayload(PacketData packetData) {
        this.data = packetData.readMap();
    }

    public static CloudSyncServerStoragePacket create(Map<String, Object> data) {
        return new CloudSyncServerStoragePacket(data);
    }
}
