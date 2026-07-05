package de.pocketcloud.cloud.network.packet.impl.response;

import de.pocketcloud.cloud.network.packet.ResponsePacket;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerNotificationCheckResponsePacket extends ResponsePacket {

    private boolean enabled;

    public PlayerNotificationCheckResponsePacket(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(enabled);
    }

    public static PlayerNotificationCheckResponsePacket create(boolean enabled) {
        return new PlayerNotificationCheckResponsePacket(enabled);
    }
}
