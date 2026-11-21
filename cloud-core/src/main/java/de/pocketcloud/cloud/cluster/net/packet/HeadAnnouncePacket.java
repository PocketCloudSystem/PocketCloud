package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The sender won the election for {@code term}.
 */
@Getter
@NoArgsConstructor
public final class HeadAnnouncePacket extends CloudPacket {

    private long term;

    public HeadAnnouncePacket(long term) {
        this.term = term;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.write(term);
    }

    @Override
    public void decodePayload(IPacketData data) {
        term = data.readLong();
    }
}
