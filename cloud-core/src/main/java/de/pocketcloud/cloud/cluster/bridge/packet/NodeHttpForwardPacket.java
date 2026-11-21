package de.pocketcloud.cloud.cluster.bridge.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head or owning node: handle an HTTP API request that was authenticated on the sender.
 */
@Getter
@NoArgsConstructor
public final class NodeHttpForwardPacket extends RequestPacket {

    private String method;
    private String uri;
    private String contentType;
    private String body;

    public NodeHttpForwardPacket(String method, String uri, String contentType, String body) {
        this.method = method;
        this.uri = uri;
        this.contentType = contentType;
        this.body = body;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(method, uri, contentType, body);
    }

    @Override
    public void decodePayload(IPacketData data) {
        method = data.readString();
        uri = data.readString();
        contentType = data.readString();
        body = data.readString();
    }
}
