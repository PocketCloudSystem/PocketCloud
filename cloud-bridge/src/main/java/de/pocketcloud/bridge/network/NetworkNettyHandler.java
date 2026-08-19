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

import java.net.SocketException;

public class NetworkNettyHandler extends SimpleChannelInboundHandler<CloudPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.IN, packet.getClass()), ctx.channel(), packet, packet.getSize());
            CloudAPI.instance().events().call(new PacketReceivedEvent(packet, ctx.channel()));
            if (packet instanceof ResponsePacket responsePacket) {
                CloudBridge.instance().requests().resolve(responsePacket);
            }

            CloudBridge.instance().packets().invokeHandlers(packet, ctx.channel());
        } catch (Exception e) {
            CloudBridge.instance().logger().exception("Unhandled exception while processing packet §b{} §rsent by §b{}§r.", e, packet.getName(), ctx.channel().remoteAddress());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof SocketException) {
            if (cause.getMessage().equals("Connection reset")) {
                CloudBridge.instance().shutdown();
                return;
            }
        }

        CloudBridge.instance().logger().exception("Unhandled exception caused by §b{}§r. §8(§renable §edebug §rto view full stack trace§8)", cause, ctx.channel().remoteAddress());
    }
}