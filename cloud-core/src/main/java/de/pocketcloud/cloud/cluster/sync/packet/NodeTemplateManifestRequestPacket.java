package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head: the manifest of the valid version of a unit. Answered with {@link HeadTemplateManifestPacket}.
 * With {@code peerCopy} (a new head asking a node) the node answers with the newest version it has itself.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateManifestRequestPacket extends RequestPacket {

    private String unit;
    private boolean peerCopy;

    public NodeTemplateManifestRequestPacket(String unit) {
        this(unit, false);
    }

    public NodeTemplateManifestRequestPacket(String unit, boolean peerCopy) {
        this.unit = unit;
        this.peerCopy = peerCopy;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(unit, peerCopy);
    }

    @Override
    public void decodePayload(IPacketData data) {
        unit = data.readString();
        peerCopy = data.readBool();
    }
}
