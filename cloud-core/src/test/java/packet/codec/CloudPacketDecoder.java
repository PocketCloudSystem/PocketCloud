package packet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import packet.util.PacketSerializer;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CloudPacketDecoder extends ByteToMessageDecoder {

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

        var packet = PacketSerializer.decode(bytes, false, "test");
        if (packet == null) {
            System.out.println("Received packet is null");
            System.out.println(new String(bytes, StandardCharsets.UTF_8));
            return;
        }

        out.add(packet);
    }
}
