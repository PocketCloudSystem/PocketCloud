package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public final class NodeStartServerResultPacket extends ResponsePacket {

    private boolean success;
    private String error;
    private int port;

    public NodeStartServerResultPacket(boolean success, String error, int port) {
        this.success = success;
        this.error = error == null ? "" : error;
        this.port = port;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(success, error, port);
    }

    @Override
    public void decodePayload(IPacketData data) {
        success = data.readBool();
        error = data.readString();
        port = data.readInt();
    }
}
