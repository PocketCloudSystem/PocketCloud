package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Owning node → all: current state of its servers.
 * <p>
 * {@code fullReport = false}: changes (status changes, new servers) plus removed server names.
 * {@code fullReport = true}: the complete list of servers of the sender (after connecting);
 * receivers drop every server of the sender that is not in it.
 */
@Getter
@NoArgsConstructor
public final class NodeServerStatePacket extends CloudPacket {

    private boolean fullReport;
    private List<Map<String, Object>> servers;
    private List<String> removed;

    public NodeServerStatePacket(boolean fullReport, List<Map<String, Object>> servers, List<String> removed) {
        this.fullReport = fullReport;
        this.servers = servers;
        this.removed = removed;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(fullReport, servers, removed);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void decodePayload(IPacketData data) {
        fullReport = data.readBool();
        servers = (List<Map<String, Object>>) (List<?>) data.readArray();
        removed = data.readArray(String.class);
    }
}
