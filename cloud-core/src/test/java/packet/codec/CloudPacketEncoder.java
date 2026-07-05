package packet.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import packet.CloudPacket;
import packet.util.PacketSerializer;

public class CloudPacketEncoder extends MessageToByteEncoder<CloudPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, CloudPacket packet, ByteBuf out) {
        byte[] bytes = PacketSerializer.encode(packet, false, "test");
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
