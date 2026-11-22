package de.pocketcloud.cloud.cluster.scheduling.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head: {@code node drain|undrain <name> [--stop]} typed (or requested via HTTP) on a node that is not head.
 * Answered with {@link HeadDrainResultPacket}.
 */
@Getter
@NoArgsConstructor
public final class NodeDrainRequestPacket extends RequestPacket {

    private String node;
    private boolean drain;
    private boolean stopServers;

    public NodeDrainRequestPacket(String node, boolean drain, boolean stopServers) {
        this.node = node;
        this.drain = drain;
        this.stopServers = stopServers;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(node, drain, stopServers);
    }

    @Override
    public void decodePayload(IPacketData data) {
        node = data.readString();
        drain = data.readBool();
        stopServers = data.readBool();
    }
}
