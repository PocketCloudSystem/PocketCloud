package de.pocketcloud.cloud.cluster.security.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Certificate renewal, step 1: a node holding {@code ca.key} asks the target node to generate a new key pair.
 * The target keeps the private key and answers with the public key ({@link NodeCertKeyResultPacket}).
 */
@Getter
@NoArgsConstructor
public final class NodeCertKeyRequestPacket extends RequestPacket {

    private String reason;

    public NodeCertKeyRequestPacket(String reason) {
        this.reason = reason == null ? "" : reason;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(reason);
    }

    @Override
    public void decodePayload(IPacketData data) {
        reason = data.readString();
    }
}
