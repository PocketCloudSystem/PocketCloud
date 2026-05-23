package de.pocketcloud.cloud.network.packet.codec;

import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.util.PacketSerializer;
import de.pocketcloud.cloud.traffic.TrafficMonitor;
import de.pocketcloud.cloud.traffic.TrafficMonitorManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

public final class CloudPacketEncoder extends MessageToByteEncoder<CloudPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, CloudPacket packet, ByteBuf out) {
        byte[] bytes = PacketSerializer.encode(packet, false, "test");
        packet.setSize(bytes.length);
        TrafficMonitorManager.getInstance().pushBytes(TrafficMonitorManager.TRAFFIC_NETWORK, bytes.length, TrafficMonitor.REGULAR_MODE_OUT);
        TrafficMonitorManager.getInstance().callHandlers(TrafficMonitorManager.TRAFFIC_NETWORK, TrafficMonitor.REGULAR_MODE_OUT, ctx.channel().remoteAddress(), new String(bytes, StandardCharsets.UTF_8), (long) packet.getSize());
        TrafficMonitorManager.getInstance().callHandlers(TrafficMonitorManager.TRAFFIC_NETWORK, TrafficMonitor.REGULAR_MODE_OUT, ctx.channel().remoteAddress(), packet, (long) packet.getSize());
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}