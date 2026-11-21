package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Head → nodes: a unit has a new valid version (hash {@code "deleted"}: the unit was removed).
 * Nodes whose copy differs fetch the manifest and the changed files.
 */
@Getter
@NoArgsConstructor
public final class HeadTemplateVersionPacket extends CloudPacket {

    private String unit;
    private long version;
    private String hash;
    /** Node whose push led to this announcement ("" if none); that node stops waiting for its push. */
    private String origin;

    public HeadTemplateVersionPacket(String unit, long version, String hash, String origin) {
        this.unit = unit;
        this.version = version;
        this.hash = hash;
        this.origin = origin == null ? "" : origin;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(unit, version, hash, origin);
    }

    @Override
    public void decodePayload(IPacketData data) {
        unit = data.readString();
        version = data.readLong();
        hash = data.readString();
        origin = data.readString();
    }
}
