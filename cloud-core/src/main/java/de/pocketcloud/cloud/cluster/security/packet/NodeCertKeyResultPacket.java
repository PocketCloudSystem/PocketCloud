package de.pocketcloud.cloud.cluster.security.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Answer to {@link NodeCertKeyRequestPacket}: the new public key (X.509 SubjectPublicKeyInfo, Base64). */
@Getter
@NoArgsConstructor
public final class NodeCertKeyResultPacket extends ResponsePacket {

    private boolean success;
    private String error;
    private String publicKey;

    public NodeCertKeyResultPacket(boolean success, String error, String publicKey) {
        this.success = success;
        this.error = error == null ? "" : error;
        this.publicKey = publicKey == null ? "" : publicKey;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(success, error, publicKey);
    }

    @Override
    public void decodePayload(IPacketData data) {
        success = data.readBool();
        error = data.readString();
        publicKey = data.readString();
    }
}
