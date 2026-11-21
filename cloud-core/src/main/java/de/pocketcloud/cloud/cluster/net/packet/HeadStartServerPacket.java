package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Head → node: start a server with the given name parts on this node.
 */
@Getter
@NoArgsConstructor
public final class HeadStartServerPacket extends RequestPacket {

    private long term;
    private String templateName;
    private int id;
    private String uuid;

    public HeadStartServerPacket(long term, String templateName, int id, String uuid) {
        this.term = term;
        this.templateName = templateName;
        this.id = id;
        this.uuid = uuid;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(term, templateName, id, uuid);
    }

    @Override
    public void decodePayload(IPacketData data) {
        term = data.readLong();
        templateName = data.readString();
        id = data.readInt();
        uuid = data.readString();
    }
}
