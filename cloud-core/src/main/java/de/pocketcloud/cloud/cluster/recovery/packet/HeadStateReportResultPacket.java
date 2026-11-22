package de.pocketcloud.cloud.cluster.recovery.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Head → node: the decision for each reported server, keyed by uuid: {@code ADOPT} or {@code KILL}.
 */
@Getter
@NoArgsConstructor
public final class HeadStateReportResultPacket extends CloudPacket {

    private long term;
    private Map<String, String> decisions;

    public HeadStateReportResultPacket(long term, Map<String, String> decisions) {
        this.term = term;
        this.decisions = decisions;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(term, decisions);
    }

    @Override
    public void decodePayload(IPacketData data) {
        term = data.readLong();
        decisions = data.readMap(String.class);
    }
}
