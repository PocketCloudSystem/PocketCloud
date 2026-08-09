package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ProxyRegisterServerPacket extends CloudPacket implements ClientboundPacket {

    private String serverName;
    private String address;
    private int port;

    public ProxyRegisterServerPacket(String serverName, String address, int port) {
        this.serverName = serverName != null ? serverName : "";
        this.address = address;
        this.port = port;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(serverName, address, port);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        serverName = packetData.readString();
        address = packetData.readString();
        port = packetData.readInt();
    }

    public static ProxyRegisterServerPacket create(String serverName, String address, int port) {
        return new ProxyRegisterServerPacket(serverName, address, port);
    }

    public static ProxyRegisterServerPacket create(ICloudServer server) {
        return create(server.name(), server.data().address(), server.data().port());
    }
}
