package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ProxyUnregisterServerPacket extends CloudPacket implements ClientboundPacket {

    private String serverName;

    public ProxyUnregisterServerPacket(String serverName) {
        this.serverName = serverName != null ? serverName : "";
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(serverName);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static ProxyUnregisterServerPacket create(String serverName) {
        return new ProxyUnregisterServerPacket(serverName);
    }

    public static ProxyUnregisterServerPacket create(CloudServer server) {
        return new ProxyUnregisterServerPacket(server.name());
    }
}
