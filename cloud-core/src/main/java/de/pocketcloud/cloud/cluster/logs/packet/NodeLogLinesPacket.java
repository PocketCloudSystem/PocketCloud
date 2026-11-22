package de.pocketcloud.cloud.cluster.logs.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Owning node → subscriber: new log lines of a server (cluster spec: NodeLogLines), batched.
 * {@code ended = true}: the server stopped, the subscription is over.
 */
@Getter
@NoArgsConstructor
public final class NodeLogLinesPacket extends CloudPacket {

    private String serverName;
    private List<String> lines;
    private boolean ended;

    public NodeLogLinesPacket(String serverName, List<String> lines, boolean ended) {
        this.serverName = serverName;
        this.lines = lines;
        this.ended = ended;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(serverName, lines, ended);
    }

    @Override
    public void decodePayload(IPacketData data) {
        serverName = data.readString();
        lines = data.readArray(String.class);
        ended = data.readBool();
    }
}
