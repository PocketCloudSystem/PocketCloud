package de.pocketcloud.cloud.cluster.bridge.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Node → all: players changed on the sender (joined, switched server, left).
 * {@code fullReport = true}: all players owned by the sender (sent after connecting).
 */
@Getter
@NoArgsConstructor
public final class NodePlayerStatePacket extends CloudPacket {

    private boolean fullReport;
    private List<Map<String, Object>> players;
    private List<String> removed;

    public NodePlayerStatePacket(boolean fullReport, List<Map<String, Object>> players, List<String> removed) {
        this.fullReport = fullReport;
        this.players = players;
        this.removed = removed;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(fullReport, players, removed);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void decodePayload(IPacketData data) {
        fullReport = data.readBool();
        players = (List<Map<String, Object>>) (List<?>) data.readArray();
        removed = data.readArray(String.class);
    }
}
