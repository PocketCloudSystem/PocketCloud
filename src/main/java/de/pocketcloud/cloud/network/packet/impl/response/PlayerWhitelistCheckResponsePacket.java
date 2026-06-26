package de.pocketcloud.cloud.network.packet.impl.response;

import de.pocketcloud.cloud.network.packet.ResponsePacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerWhitelistCheckResponsePacket extends ResponsePacket {

    private boolean whitelisted;

    public PlayerWhitelistCheckResponsePacket(boolean whitelisted) {
        this.whitelisted = whitelisted;
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(whitelisted);
    }

    public static PlayerWhitelistCheckResponsePacket create(boolean whitelisted) {
        return new PlayerWhitelistCheckResponsePacket(whitelisted);
    }
}
