package de.pocketcloud.bridge.platform.pnx.handler;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.bridge.command.sender.PNXCloudCommandSender;
import de.pocketcloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import de.pocketcloud.shared.network.packet.type.ServerCommandExecutionResult;

public final class ServerPacketHandler implements PacketListener {

    @PacketHandler(CommandExecuteRequestPacket.class)
    public void handle(CommandExecuteRequestPacket packet) {
        PNXCloudCommandSender pnxSender = new PNXCloudCommandSender(packet.getId());
        pnxSender.getServer().executeCommand(pnxSender, packet.getCommandLine());
        pnxSender.getServer().getScheduler().scheduleDelayedTask(() -> packet.sendResponse(CommandExecuteResponsePacket.create(new ServerCommandExecutionResult(
                packet.getId(),
                packet.getCommandLine(),
                pnxSender.getCachedMessages()
        ))), 20);
    }
}