package de.pocketcloud.network.packet.impl.request.client;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class CommandExecuteRequestPacket extends RequestPacket implements ClientboundPacket {

    private String commandLine;
    private String id;

    public CommandExecuteRequestPacket(String commandLine, String id) {
        this.commandLine = commandLine != null ? commandLine : "";
        this.id = id != null ? id : "";
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(commandLine, id);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        commandLine = packetData.readString();
        id = packetData.readString();
    }

    public static CommandExecuteRequestPacket create(String commandLine, String id) {
        return new CommandExecuteRequestPacket(commandLine, id);
    }
}
