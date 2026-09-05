package de.pocketcloud.cloud.network;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.packet.PacketReceiveEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import de.pocketcloud.shared.event.network.PacketReceivedEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;
import java.time.Instant;

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
            PocketCloud.instance().traffic().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.IN, packet.getClass()), ctx.channel(), packet, packet.getSize());
            CloudServer server = null;
            ServerClient c = ctx.channel().attr(ServerClient.ATTRIBUTE_KEY).get();

            if (packet instanceof AuthenticatedPacket) {
                if (c.server() == null) return;
                else server = c.server();
            }

            if (new PacketReceiveEvent(ctx.channel(), (CloudboundPacket) packet).call().isCancelled()) {
                return;
            }

            CloudAPI.instance().events().call(new PacketReceivedEvent(packet, ctx.channel()));

            if (packet instanceof ResponsePacket responsePacket) {
                PocketCloud.instance().requests().resolve(responsePacket);
            }

            PocketCloud.instance().packets().invokeHandlers(packet, c);

            if (server != null) server.latestPacketInfo().setLatestPacket(Instant.now(), packet.getClass());
        } catch (Exception e) {
            boolean debugEnabled = CloudLogger.get().isDebugMode();
            CloudLogger.get().error("Unhandled exception while processing packet §b{} §rsent by §b{}§r.{}", packet.getName(), ctx.channel().remoteAddress(), debugEnabled ? "" : " §8(§renable §edebug §rto view full stack trace§8)");
            if (debugEnabled) CloudLogger.get().exception(e);
            else CloudLogger.get().error(e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            disconnectChannel(ctx.channel());
            ctx.close();
            return;
        }

        boolean debugEnabled = CloudLogger.get().isDebugMode();
        CloudLogger.get().error("Unhandled exception caused by §b{}§r.{}", ctx.channel().remoteAddress(), debugEnabled ? "" : " §8(§renable §edebug §rto view full stack trace§8)");
        if (debugEnabled) CloudLogger.get().exception(cause);
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
            PocketCloud.instance().clients().remove(client);
        }
    }
}