package de.pocketcloud.cloud.cluster.bridge.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public final class NodeServerActionResultPacket extends ResponsePacket {

    private boolean success;
    private String error;
    private List<String> lines;
    private Map<String, Object> data;

    public NodeServerActionResultPacket(boolean success, String error, List<String> lines, Map<String, Object> data) {
        this.success = success;
        this.error = error;
        this.lines = lines;
        this.data = data;
    }

    public static NodeServerActionResultPacket ok(List<String> lines, Map<String, Object> data) {
        return new NodeServerActionResultPacket(true, null, lines, data);
    }

    public static NodeServerActionResultPacket failed(String error) {
        return new NodeServerActionResultPacket(false, error, null, null);
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(success, error, lines, data);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        success = packetData.readBool();
        error = packetData.readString();
        lines = packetData.peek() == null ? NodeServerActionPacket.nullValue(packetData) : packetData.readArray(String.class);
        data = packetData.peek() == null ? NodeServerActionPacket.nullValue(packetData) : packetData.readMap();
    }
}
