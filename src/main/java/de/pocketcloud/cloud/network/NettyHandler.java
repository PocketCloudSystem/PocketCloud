package de.pocketcloud.cloud.network;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.ResponseClientPacket;
import de.pocketcloud.cloud.network.request.RequestManager;
import de.pocketcloud.cloud.traffic.TrafficMonitor;
import de.pocketcloud.cloud.traffic.TrafficMonitorManager;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketException;

public class NettyHandler extends SimpleChannelInboundHandler<CloudPacket> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        PocketCloud.getInstance().network().addChannel(channel);
        ctx.channel().attr(ServerClient.ATTRIBUTE_KEY).set(new ServerClient(channel));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            TrafficMonitorManager.getInstance().callHandlers(TrafficMonitorManager.TRAFFIC_NETWORK, TrafficMonitor.REGULAR_MODE_IN, ctx.channel().remoteAddress(), packet, (long) packet.getSize());
            if (packet instanceof ResponseClientPacket responseClientPacket) {
                RequestManager.getInstance().resolve(responseClientPacket);
                return;
            }

            packet.handle();
        } catch (Exception e) {
            CloudLogger.get().error("Unhandled exception while processing packet {} from {}: {} §8(§renable §edebug §rto view full stack trace§8)", packet.getName(), ctx.channel().remoteAddress(), e.getMessage());
            if (CloudLogger.get().isDebugMode()) CloudLogger.get().exception(e);
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

        CloudLogger.get().error("Unhandled exception caused by {}: {} §8(§renable §edebug §rto view full stack trace§8)", ctx.channel().remoteAddress(), cause.getMessage());
        if (CloudLogger.get().isDebugMode()) CloudLogger.get().exception(cause);
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        disconnectChannel(ctx.channel());
    }

    private void disconnectChannel(Channel channel) {
        PocketCloud.getInstance().network().removeChannel(channel);
        ServerClient client = channel.attr(ServerClient.ATTRIBUTE_KEY).get();
        if (client != null) {
            ServerClientCache.getInstance().remove(client);
        }
    }
}