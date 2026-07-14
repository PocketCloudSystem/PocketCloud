package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ProxyUnregisterServerPacket extends CloudPacket implements ClientboundPacket {

    private String serverName;

    public ProxyUnregisterServerPacket(String serverName) {
        this.serverName = serverName != null ? serverName : "";
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(serverName);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static ProxyUnregisterServerPacket create(String serverName) {
        return new ProxyUnregisterServerPacket(serverName);
    }

    public static ProxyUnregisterServerPacket create(ICloudServer server) {
        return new ProxyUnregisterServerPacket(server.name());
    }
}
