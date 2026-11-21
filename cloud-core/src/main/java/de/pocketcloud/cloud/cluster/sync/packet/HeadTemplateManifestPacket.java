package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Head → node: valid version of a unit. {@code manifest} is the gzip+base64 encoded manifest.
 */
@Getter
@NoArgsConstructor
public final class HeadTemplateManifestPacket extends ResponsePacket {

    private boolean found;
    private long version;
    private boolean deleted;
    private String manifest;

    public HeadTemplateManifestPacket(boolean found, long version, boolean deleted, String manifest) {
        this.found = found;
        this.version = version;
        this.deleted = deleted;
        this.manifest = manifest == null ? "" : manifest;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(found, version, deleted, manifest);
    }

    @Override
    public void decodePayload(IPacketData data) {
        found = data.readBool();
        version = data.readLong();
        deleted = data.readBool();
        manifest = data.readString();
    }
}
