package de.pocketcloud.bridge.platform.wdpe.handler;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.bridge.command.sender.WDPECloudCommandSender;
import de.pocketcloud.network.packet.impl.PlayerTransferPacket;
import de.pocketcloud.network.packet.impl.ProxyRegisterServerPacket;
import de.pocketcloud.network.packet.impl.ProxyUnregisterServerPacket;
import de.pocketcloud.network.packet.impl.request.client.CommandExecuteRequestPacket;
import de.pocketcloud.network.packet.impl.response.client.CommandExecuteResponsePacket;
import de.pocketcloud.shared.network.packet.type.ServerCommandExecutionResult;
import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.network.serverinfo.BedrockServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

import java.net.InetSocketAddress;

public final class ProxyPacketHandler implements PacketListener {

    @PacketHandler(CommandExecuteRequestPacket.class)
    public void handle(CommandExecuteRequestPacket packet) {
        WDPECloudCommandSender wdpeSender = new WDPECloudCommandSender(packet.getId());
        wdpeSender.getProxy().dispatchCommand(wdpeSender, packet.getCommandLine());
        packet.sendResponse(CommandExecuteResponsePacket.create(new ServerCommandExecutionResult(packet.getId(), packet.getCommandLine(), wdpeSender.getCachedMessages())));
    }

    @PacketHandler(ProxyRegisterServerPacket.class)
    public void handle(ProxyRegisterServerPacket packet) {
        if (ProxyServer.getInstance().getServerInfo(packet.getServerName()) != null)
            ProxyServer.getInstance().removeServerInfo(packet.getServerName());
        ProxyServer.getInstance().registerServerInfo(new BedrockServerInfo(
                packet.getServerName(),
                new InetSocketAddress(packet.getAddress(), packet.getPort()),
                null
        ));
    }

    @PacketHandler(ProxyUnregisterServerPacket.class)
    public void handle(ProxyUnregisterServerPacket packet) {
        ProxyServer.getInstance().removeServerInfo(packet.getServerName());
    }

    @PacketHandler(PlayerTransferPacket.class)
    public void handle(PlayerTransferPacket packet) {
        ProxiedPlayer player = ProxyServer.getInstance().getPlayer(packet.getPlayer());
        ServerInfo serverInfo = ProxyServer.getInstance().getServerInfo(packet.getServer());
        if (player != null && serverInfo != null) {
            player.redirectServer(serverInfo);
        }
    }
}