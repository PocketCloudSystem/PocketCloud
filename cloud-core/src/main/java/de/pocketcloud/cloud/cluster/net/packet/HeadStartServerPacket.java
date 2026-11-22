package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Head → node: start a server with the given name parts on this node.
 * {@code versions} are the template versions ({@code unit → "version:hash"}) the node must have before it
 * prepares the server (see {@code TemplateReplicator#ensureVersions}).
 */
@Getter
@NoArgsConstructor
public final class HeadStartServerPacket extends RequestPacket {

    private long term;
    private String templateName;
    private int id;
    private String uuid;
    private Map<String, Object> versions;

    public HeadStartServerPacket(long term, String templateName, int id, String uuid, Map<String, Object> versions) {
        this.term = term;
        this.templateName = templateName;
        this.id = id;
        this.uuid = uuid;
        this.versions = versions == null ? Map.of() : versions;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(term, templateName, id, uuid, versions);
    }

    @Override
    public void decodePayload(IPacketData data) {
        term = data.readLong();
        templateName = data.readString();
        id = data.readInt();
        uuid = data.readString();
        versions = data.readMap();
    }
}
