package de.pocketcloud.cloud.cluster.security.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Certificate renewal, step 2: the issuing node sends the certificate it signed for the key of step 1.
 * The target checks and installs it and answers with {@link NodeCertInstallResultPacket}.
 */
@Getter
@NoArgsConstructor
public final class NodeCertInstallPacket extends RequestPacket {

    private String certificatePem;

    public NodeCertInstallPacket(String certificatePem) {
        this.certificatePem = certificatePem;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(certificatePem);
    }

    @Override
    public void decodePayload(IPacketData data) {
        certificatePem = data.readString();
    }
}
