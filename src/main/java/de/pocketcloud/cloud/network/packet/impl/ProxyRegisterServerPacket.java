package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ProxyRegisterServerPacket extends CloudPacket implements ClientboundPacket {

    private String serverName;
    private int port;

    public ProxyRegisterServerPacket(String serverName, int port) {
        this.serverName = serverName != null ? serverName : "";
        this.port = port;
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(serverName, port);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static ProxyRegisterServerPacket create(String serverName, int port) {
        return new ProxyRegisterServerPacket(serverName, port);
    }

    public static ProxyRegisterServerPacket create(CloudServer server) {
        return create(server.name(), server.serverData().port());
    }
}
