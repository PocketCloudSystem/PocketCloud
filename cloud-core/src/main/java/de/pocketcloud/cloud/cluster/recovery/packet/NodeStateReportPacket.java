package de.pocketcloud.cloud.cluster.recovery.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Node → all (answered by the head): servers of the last run of this node that are still running
 * and wait for a decision whether the node may take them over. See {@code ServerRecovery}.
 * Each entry: name, uuid, template, id, port, pid, status.
 */
@Getter
@NoArgsConstructor
public final class NodeStateReportPacket extends CloudPacket {

    private List<Map<String, Object>> servers;

    public NodeStateReportPacket(List<Map<String, Object>> servers) {
        this.servers = servers;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(servers);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void decodePayload(IPacketData data) {
        servers = (List<Map<String, Object>>) (List<?>) data.readArray();
    }
}
