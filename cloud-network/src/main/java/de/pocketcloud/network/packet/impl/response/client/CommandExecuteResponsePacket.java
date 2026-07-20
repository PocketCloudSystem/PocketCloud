package de.pocketcloud.network.packet.impl.response.client;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.shared.network.packet.type.ServerCommandExecutionResult;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class CommandExecuteResponsePacket extends ResponsePacket implements AuthenticatedPacket, CloudboundPacket {

    private ServerCommandExecutionResult commandExecutionResult;

    public CommandExecuteResponsePacket(ServerCommandExecutionResult commandExecutionResult) {
        this.commandExecutionResult = commandExecutionResult;
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.commandExecutionResult = ServerCommandExecutionResult.read(packetData.readMap());
    }

    public static CommandExecuteResponsePacket create(ServerCommandExecutionResult commandExecutionResult) {
        return new CommandExecuteResponsePacket(commandExecutionResult);
    }
}