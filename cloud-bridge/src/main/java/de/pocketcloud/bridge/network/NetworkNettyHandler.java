package de.pocketcloud.bridge.network;

import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import dev.waterdog.waterdogpe.ProxyServer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.net.SocketException;

public class NetworkNettyHandler extends SimpleChannelInboundHandler<CloudPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.IN, packet.getClass()), ctx.channel(), packet, packet.getSize());
            if (packet instanceof ResponsePacket responsePacket) {
                CloudBridge.instance().requests().resolve(responsePacket);
            }

            //packet.handle(ctx.channel());
        } catch (Exception e) {
            CloudBridge.instance().logger().error("Unhandled exception while processing packet §b{} §rsent by §b{}§r. §8(§renable §edebug §rto view full stack trace§8)", packet.getName(), ctx.channel().remoteAddress());
            if (ProxyServer.getInstance().getConfiguration().isDebug()) CloudBridge.instance().logger().error("Exception:", e);
            else CloudBridge.instance().logger().error(e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SocketException) {
            if (cause.getMessage().equals("Connection reset")) {
                ProxyServer.getInstance().shutdown();
                return;
            }
        }

        CloudBridge.instance().logger().error("Unhandled exception caused by §b{}§r. §8(§renable §edebug §rto view full stack trace§8)", ctx.channel().remoteAddress());
        if (ProxyServer.getInstance().getConfiguration().isDebug()) CloudBridge.instance().logger().error("Exception:", cause);
        else CloudBridge.instance().logger().error(cause.getMessage());
    }
}