package de.pocketcloud.cloud.network.packet.codec;

import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.util.PacketSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class CloudPacketEncoder extends MessageToByteEncoder<CloudPacket> {

    @Override
    protected void encode(ChannelHandlerContext ctx, CloudPacket packet, ByteBuf out) throws Exception {
        byte[] bytes = PacketSerializer.encode(packet, false, "test");
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}
