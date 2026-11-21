package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public final class HeadVotePacket extends ResponsePacket {

    private long term;
    private boolean granted;

    public HeadVotePacket(long term, boolean granted) {
        this.term = term;
        this.granted = granted;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(term, granted);
    }

    @Override
    public void decodePayload(IPacketData data) {
        term = data.readLong();
        granted = data.readBool();
    }
}
