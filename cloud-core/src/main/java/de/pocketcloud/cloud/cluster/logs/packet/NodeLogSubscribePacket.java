package de.pocketcloud.cloud.cluster.logs.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → owning node: stream the log of one of your servers to me ({@link NodeLogLinesPacket}).
 * Sent again every few seconds to keep the subscription alive (with {@code backlog = 0}).
 * Answered with {@link NodeLogSubscribeResultPacket}, which carries the last {@code backlog} lines.
 */
@Getter
@NoArgsConstructor
public final class NodeLogSubscribePacket extends RequestPacket {

    private String serverName;
    private int backlog;

    public NodeLogSubscribePacket(String serverName, int backlog) {
        this.serverName = serverName;
        this.backlog = backlog;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(serverName, backlog);
    }

    @Override
    public void decodePayload(IPacketData data) {
        serverName = data.readString();
        backlog = data.readInt();
    }
}
