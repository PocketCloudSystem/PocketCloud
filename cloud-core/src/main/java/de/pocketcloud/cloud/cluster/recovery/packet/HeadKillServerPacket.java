package de.pocketcloud.cloud.cluster.recovery.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Head → owning node: this server was written off (or its name belongs to another server now), kill it.
 * Only applies if the local server still has that uuid.
 */
@Getter
@NoArgsConstructor
public final class HeadKillServerPacket extends CloudPacket {

    private String serverName;
    private String uuid;
    private String reason;

    public HeadKillServerPacket(String serverName, String uuid, String reason) {
        this.serverName = serverName;
        this.uuid = uuid;
        this.reason = reason;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(serverName, uuid, reason);
    }

    @Override
    public void decodePayload(IPacketData data) {
        serverName = data.readString();
        uuid = data.readString();
        reason = data.readString();
    }
}
