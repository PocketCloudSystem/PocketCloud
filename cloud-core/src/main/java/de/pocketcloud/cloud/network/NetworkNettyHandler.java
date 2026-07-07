package de.pocketcloud.cloud.network;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.packet.PacketReceiveEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.ResponseClientPacket;
import de.pocketcloud.cloud.network.request.RequestManager;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketException;

public class NetworkNettyHandler extends SimpleChannelInboundHandler<CloudPacket> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        PocketCloud.instance().network().addChannel(channel);
        ctx.channel().attr(ServerClient.ATTRIBUTE_KEY).set(new ServerClient(channel));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.IN, packet.getClass()), ctx.channel(), packet, packet.getSize());

            if (packet instanceof AuthenticatedPacket) {
                ServerClient c = ctx.channel().attr(ServerClient.ATTRIBUTE_KEY).get();
                if (c.server() == null) return;
            }

            if (new PacketReceiveEvent(ctx.channel(), (CloudboundPacket) packet).call().isCancelled()) {
                return;
            }

            if (packet instanceof ResponseClientPacket responseClientPacket) {
                RequestManager.instance().resolve(responseClientPacket);
                return;
            }

            packet.handle(ctx.channel());
        } catch (Exception e) {
            CloudLogger.get().error("Unhandled exception while processing packet §b{} §rsent by §b{}§r. §8(§renable §edebug §rto view full stack trace§8)", packet.getName(), ctx.channel().remoteAddress());
            if (CloudLogger.get().isDebugMode()) CloudLogger.get().exception(e);
            else CloudLogger.get().error(e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SocketException) {
            if (cause.getMessage().equals("Connection reset")) {
                disconnectChannel(ctx.channel());
                return;
            }
        }

        CloudLogger.get().error("Unhandled exception caused by §b{}§r. §8(§renable §edebug §rto view full stack trace§8)", ctx.channel().remoteAddress());
        if (CloudLogger.get().isDebugMode()) CloudLogger.get().exception(cause);
        else CloudLogger.get().error(cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        disconnectChannel(ctx.channel());
    }

    private void disconnectChannel(Channel channel) {
        PocketCloud.instance().network().removeChannel(channel);
        ServerClient client = channel.attr(ServerClient.ATTRIBUTE_KEY).get();
        if (client != null) {
            ServerClientCache.instance().remove(client);
        }
    }
}