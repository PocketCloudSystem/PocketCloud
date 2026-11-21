package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head: the manifest of the valid version of a unit. Answered with {@link HeadTemplateManifestPacket}.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateManifestRequestPacket extends RequestPacket {

    private String unit;

    public NodeTemplateManifestRequestPacket(String unit) {
        this.unit = unit;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.write(unit);
    }

    @Override
    public void decodePayload(IPacketData data) {
        unit = data.readString();
    }
}
