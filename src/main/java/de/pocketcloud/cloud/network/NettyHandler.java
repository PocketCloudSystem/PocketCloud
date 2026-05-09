package de.pocketcloud.cloud.network;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketException;

public class NettyHandler extends SimpleChannelInboundHandler<CloudPacket> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CloudLogger.get().info("Client connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            packet.handle();
        } catch (Exception e) {
            CloudLogger.get().error("Unhandled exception while processing packet {} from {}: {}", packet.getName(), ctx.channel().remoteAddress(), e.getMessage());
            if (CloudLogger.get().isDebugMode()) CloudLogger.get().exception(e);
        }

        ctx.writeAndFlush(packet);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SocketException) {
            if (cause.getMessage().equals("Connection reset")) {
                CloudLogger.get().info("Client disconnected: {}", ctx.channel().remoteAddress());
                return;
            }
        }

        CloudLogger.get().error("Unhandled exception in pipeline for {}: {}", ctx.channel().remoteAddress(), cause.getMessage());
        if (CloudLogger.get().isDebugMode()) CloudLogger.get().exception(cause);
        ctx.close();
    }
}
