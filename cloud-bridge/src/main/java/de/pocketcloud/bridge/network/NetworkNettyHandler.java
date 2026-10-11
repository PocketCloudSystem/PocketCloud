package de.pocketcloud.bridge.network;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import de.pocketcloud.shared.event.network.PacketReceivedEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.IOException;

public class NetworkNettyHandler extends SimpleChannelInboundHandler<CloudPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.IN, packet.getClass()), ctx.channel(), packet, packet.getSize());
        } catch (Exception e) {
            CloudBridge.instance().logger().exception("Unhandled exception while handling traffic §rsent by §b{}§r.", e, ctx.channel().remoteAddress());
        }

        CloudBridge.instance().platformPlugin().startTask(() -> process(ctx, packet), 0, 0);
    }

    private void process(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            CloudAPI.instance().events().call(new PacketReceivedEvent(packet, ctx.channel()));
            if (packet instanceof ResponsePacket responsePacket) CloudBridge.instance().requests().resolve(responsePacket);
            CloudBridge.instance().packets().invokeHandlers(packet, ctx.channel());
        } catch (Exception e) {
            CloudBridge.instance().logger().exception("Unhandled exception while processing packet §b{} §rsent by §b{}§r.", e, packet.getName(), ctx.channel().remoteAddress());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        CloudBridge.instance().logger().warn("Connection to the cloud was lost, shutting down...");
        CloudBridge.instance().shutdown();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            ctx.close();
            return;
        }

        CloudBridge.instance().logger().exception("Unhandled exception caused by §b{}§r.", cause, ctx.channel().remoteAddress());
    }
}