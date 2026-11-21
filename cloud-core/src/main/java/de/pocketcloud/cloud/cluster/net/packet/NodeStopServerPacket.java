package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Any node → owning node: stop one of your servers.
 */
@Getter
@NoArgsConstructor
public final class NodeStopServerPacket extends CloudPacket {

    private String serverName;
    private boolean force;

    public NodeStopServerPacket(String serverName, boolean force) {
        this.serverName = serverName;
        this.force = force;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(serverName, force);
    }

    @Override
    public void decodePayload(IPacketData data) {
        serverName = data.readString();
        force = data.readBool();
    }
}
