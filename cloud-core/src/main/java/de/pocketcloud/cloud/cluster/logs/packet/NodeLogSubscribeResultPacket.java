package de.pocketcloud.cloud.cluster.logs.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** Answer to {@link NodeLogSubscribePacket}: the requested backlog, or why the log cannot be streamed. */
@Getter
@NoArgsConstructor
public final class NodeLogSubscribeResultPacket extends ResponsePacket {

    private boolean success;
    private String error;
    private List<String> lines;

    public NodeLogSubscribeResultPacket(boolean success, String error, List<String> lines) {
        this.success = success;
        this.error = error == null ? "" : error;
        this.lines = lines == null ? List.of() : lines;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(success, error, lines);
    }

    @Override
    public void decodePayload(IPacketData data) {
        success = data.readBool();
        error = data.readString();
        lines = data.readArray(String.class);
    }
}
