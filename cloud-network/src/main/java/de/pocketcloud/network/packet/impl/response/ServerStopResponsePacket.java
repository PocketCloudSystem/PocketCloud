package de.pocketcloud.network.packet.impl.response;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.shared.component.BaseCloudServer;
import de.pocketcloud.shared.network.packet.type.ActionFailureReason;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;

@NoArgsConstructor
@Getter
public final class ServerStopResponsePacket extends ResponsePacket implements ClientboundPacket {

    private ActionFailureReason errorReason;
    private Collection<? extends ICloudServer> affectedServers;

    public ServerStopResponsePacket(ActionFailureReason errorReason, Collection<? extends ICloudServer> affectedServers) {
        this.errorReason = errorReason;
        this.affectedServers = affectedServers;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(errorReason, affectedServers);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        errorReason = packetData.readEnum(ActionFailureReason.class);
        affectedServers = packetData.readArray(BaseCloudServer.class);
    }

    public static ServerStopResponsePacket create(ActionFailureReason errorReason, Collection<? extends ICloudServer> affectedServers) {
        return new ServerStopResponsePacket(errorReason, affectedServers);
    }
}
