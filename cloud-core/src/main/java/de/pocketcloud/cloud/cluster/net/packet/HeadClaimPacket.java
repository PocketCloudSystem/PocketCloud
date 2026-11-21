package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A candidate asks for votes to become head in {@code term}.
 */
@Getter
@NoArgsConstructor
public final class HeadClaimPacket extends RequestPacket {

    private long term;
    private long startedAt;

    public HeadClaimPacket(long term, long startedAt) {
        this.term = term;
        this.startedAt = startedAt;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(term, startedAt);
    }

    @Override
    public void decodePayload(IPacketData data) {
        term = data.readLong();
        startedAt = data.readLong();
    }
}
