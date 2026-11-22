package de.pocketcloud.cloud.cluster.security.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Answer to {@link NodeCertInstallPacket}. */
@Getter
@NoArgsConstructor
public final class NodeCertInstallResultPacket extends ResponsePacket {

    private boolean success;
    private String error;

    public NodeCertInstallResultPacket(boolean success, String error) {
        this.success = success;
        this.error = error == null ? "" : error;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(success, error);
    }

    @Override
    public void decodePayload(IPacketData data) {
        success = data.readBool();
        error = data.readString();
    }
}
