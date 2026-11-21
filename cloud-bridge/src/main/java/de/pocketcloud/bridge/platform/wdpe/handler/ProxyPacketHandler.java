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
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfo;
import dev.waterdog.waterdogpe.network.serverinfo.ServerInfoType;
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
        ProxyServer proxy = ProxyServer.getInstance();
        if (proxy.getServerInfo(packet.getServerName()) != null)
            proxy.removeServerInfo(packet.getServerName());

        ServerInfoType type = ServerInfoType.fromString(packet.getTransport());
        if (type == null) type = ServerInfoType.BEDROCK;

        proxy.registerServerInfo(type.getServerInfoFactory().createServerInfo(
                packet.getServerName(),
                new InetSocketAddress(packet.getAddress(), packet.getPort()),
                null));
        proxy.getLogger().info("Registered server " + packet.getServerName() + " (" + packet.getAddress() + ":" + packet.getPort() + ")");
    }

    @PacketHandler(ProxyUnregisterServerPacket.class)
    public void handle(ProxyUnregisterServerPacket packet) {
        if (ProxyServer.getInstance().removeServerInfo(packet.getServerName()) != null) {
            ProxyServer.getInstance().getLogger().info("Unregistered server " + packet.getServerName());
        }
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