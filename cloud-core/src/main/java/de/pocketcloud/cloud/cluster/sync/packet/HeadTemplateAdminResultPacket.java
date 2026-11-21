package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/** Console lines produced on the head for {@link NodeTemplateAdminPacket}. */
@Getter
@NoArgsConstructor
public final class HeadTemplateAdminResultPacket extends ResponsePacket {

    private List<String> lines;

    public HeadTemplateAdminResultPacket(List<String> lines) {
        this.lines = lines;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.write(lines);
    }

    @Override
    public void decodePayload(IPacketData data) {
        lines = data.readArray(String.class);
    }
}
