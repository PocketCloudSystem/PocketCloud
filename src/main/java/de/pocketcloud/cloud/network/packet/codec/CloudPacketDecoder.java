package de.pocketcloud.cloud.network.packet.codec;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.util.PacketSerializer;
import de.pocketcloud.cloud.traffic.TrafficMonitor;
import de.pocketcloud.cloud.traffic.TrafficMonitorManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class CloudPacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) return;

        in.markReaderIndex();
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        TrafficMonitorManager.getInstance().pushBytes(TrafficMonitorManager.TRAFFIC_NETWORK, length, TrafficMonitor.REGULAR_MODE_IN);
        TrafficMonitorManager.getInstance().callHandlers(TrafficMonitorManager.TRAFFIC_NETWORK, TrafficMonitor.REGULAR_MODE_IN, ctx.channel().remoteAddress(), new String(bytes, StandardCharsets.UTF_8), (long) length);
        var packet = PacketSerializer.decode(bytes, false, "test");
        if (packet == null) {
            CloudLogger.get().debug("Received unknown packet from {}", ctx.channel().remoteAddress())
                    .debug(new String(bytes, StandardCharsets.UTF_8));
            return;
        }

        packet.setSize(length);
        out.add(packet);
    }
}