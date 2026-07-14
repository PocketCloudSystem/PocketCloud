package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(serverName, port);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static ProxyRegisterServerPacket create(String serverName, int port) {
        return new ProxyRegisterServerPacket(serverName, port);
    }

    public static ProxyRegisterServerPacket create(ICloudServer server) {
        return create(server.name(), server.data().port());
    }
}
