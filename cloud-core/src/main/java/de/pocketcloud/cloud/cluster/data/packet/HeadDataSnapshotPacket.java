package de.pocketcloud.cloud.cluster.data.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * The head's whitelist and notification list and, only with a provider whose files are not replicated (MySQL),
 * its templates and groups. One JSON document.
 */
@Getter
@NoArgsConstructor
public final class HeadDataSnapshotPacket extends ResponsePacket {

    private String snapshot;

    public HeadDataSnapshotPacket(String snapshot) {
        this.snapshot = snapshot == null ? "{}" : snapshot;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(snapshot);
    }

    @Override
    public void decodePayload(IPacketData data) {
        snapshot = data.readString();
    }
}
