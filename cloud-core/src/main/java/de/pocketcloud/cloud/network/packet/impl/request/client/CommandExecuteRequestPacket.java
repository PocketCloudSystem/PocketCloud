package de.pocketcloud.cloud.network.packet.impl.request.client;

import de.pocketcloud.cloud.network.packet.RequestClientPacket;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class CommandExecuteRequestPacket extends RequestClientPacket {

    private String commandLine;
    private String id;

    public CommandExecuteRequestPacket(String commandLine, String id) {
        this.commandLine = commandLine != null ? commandLine : "";
        this.id = id != null ? id : "";
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(commandLine, id);
    }

    public static CommandExecuteRequestPacket create(String commandLine, String id) {
        return new CommandExecuteRequestPacket(commandLine, id);
    }
}
