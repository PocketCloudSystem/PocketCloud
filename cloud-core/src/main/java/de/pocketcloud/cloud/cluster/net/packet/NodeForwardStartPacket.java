package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head: a start was requested on a node that is not head (console, API, bridge).
 */
@Getter
@NoArgsConstructor
public final class NodeForwardStartPacket extends RequestPacket {

    private String templateName;
    private int count;

    public NodeForwardStartPacket(String templateName, int count) {
        this.templateName = templateName;
        this.count = count;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(templateName, count);
    }

    @Override
    public void decodePayload(IPacketData data) {
        templateName = data.readString();
        count = data.readInt();
    }
}
