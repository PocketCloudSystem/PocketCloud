package de.pocketcloud.bridge.platform.pnx.handler;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.command.sender.PNXCloudCommandSender;
import de.pocketcloud.network.packet.impl.PlayerTransferPacket;
import de.pocketcloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import de.pocketcloud.shared.network.packet.type.ServerCommandExecutionResult;
import org.powernukkitx.Player;
import org.powernukkitx.Server;

import java.net.InetSocketAddress;

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

    @PacketHandler(PlayerTransferPacket.class)
    public void handle(PlayerTransferPacket packet) {
        ICloudServer server = CloudBridge.instance().servers().get(packet.getServer()).orElse(null);
        if (server == null) return;
        Player player = Server.getInstance().getPlayerExact(packet.getPlayer());
        if (player == null) return;
        player.transfer(new InetSocketAddress(server.data().address(), server.data().port()));
    }
}