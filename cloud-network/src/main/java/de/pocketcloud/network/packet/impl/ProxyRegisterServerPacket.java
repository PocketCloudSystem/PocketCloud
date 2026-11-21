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
    private String transport;

    public ProxyRegisterServerPacket(String serverName, String address, int port, String transport) {
        this.serverName = serverName != null ? serverName : "";
        this.address = address;
        this.port = port;
        this.transport = transport;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(serverName, address, port, transport);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        serverName = packetData.readString();
        address = packetData.readString();
        port = packetData.readInt();
        transport = packetData.readString();
    }

    public static ProxyRegisterServerPacket create(String serverName, String address, int port, String transport) {
        return new ProxyRegisterServerPacket(serverName, address, port, transport);
    }

    public static ProxyRegisterServerPacket create(ICloudServer server) {
        return create(server.name(), server.data().address(), server.data().port(), server.template().serverSoftware().config().transport());
    }
}
