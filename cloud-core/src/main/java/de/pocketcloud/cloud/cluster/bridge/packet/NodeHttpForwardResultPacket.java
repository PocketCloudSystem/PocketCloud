package de.pocketcloud.cloud.cluster.bridge.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public final class NodeHttpForwardResultPacket extends ResponsePacket {

    private int status;
    private Map<String, Object> headers;
    private String body;

    public NodeHttpForwardResultPacket(int status, Map<String, Object> headers, String body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(status, headers, body);
    }

    @Override
    public void decodePayload(IPacketData data) {
        status = data.readInt();
        headers = data.readMap();
        body = data.readString();
    }
}
