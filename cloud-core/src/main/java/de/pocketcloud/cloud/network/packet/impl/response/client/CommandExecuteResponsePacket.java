package de.pocketcloud.cloud.network.packet.impl.response.client;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ResponseClientPacket;
import de.pocketcloud.cloud.network.packet.type.ServerCommandExecutionResult;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class CommandExecuteResponsePacket extends ResponseClientPacket {

    private ServerCommandExecutionResult commandExecutionResult;

    public CommandExecuteResponsePacket(ServerCommandExecutionResult commandExecutionResult) {
        this.commandExecutionResult = commandExecutionResult;
    }

//    @Override
//    public void handle(@NotNull ServerClient client) {
//        var server = client.server();
//        if (server != null) {
//            CloudServersHandler.handleCommandResponse(server, commandExecutionResult);
//        }
//    }


    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void decodePayload(PacketData packetData) {
        this.commandExecutionResult = ServerCommandExecutionResult.read(packetData.readMap());
    }

    public static CommandExecuteResponsePacket create(ServerCommandExecutionResult commandExecutionResult) {
        return new CommandExecuteResponsePacket(commandExecutionResult);
    }
}