package de.pocketcloud.cloud.cluster.data.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Head → all nodes: the head wrote a change to the provider (and Redis); the nodes update their in-memory state.
 * {@code kind}: whitelist, notifications (key = player, document = "true"/"false"),
 * template, group (key = name, document = JSON of {@code write()}, empty = removed).
 */
@Getter
@NoArgsConstructor
public final class HeadDataChangedPacket extends CloudPacket {

    private String kind;
    private String key;
    private String document;

    public HeadDataChangedPacket(String kind, String key, String document) {
        this.kind = kind;
        this.key = key;
        this.document = document == null ? "" : document;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(kind, key, document);
    }

    @Override
    public void decodePayload(IPacketData data) {
        kind = data.readString();
        key = data.readString();
        document = data.readString();
    }
}
