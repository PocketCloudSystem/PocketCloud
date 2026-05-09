package de.pocketcloud.cloud.network.packet.codec;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.util.PacketSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CloudPacketDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (!in.isReadable()) return;

        in.markReaderIndex();
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        var packet = PacketSerializer.decode(bytes, false, "test");
        if (packet == null) {
            CloudLogger.get().debug("Received unknown packet from {}", ctx.channel().remoteAddress())
                    .debug(new String(bytes, StandardCharsets.UTF_8));
            return;
        }

        out.add(packet);
    }
}
