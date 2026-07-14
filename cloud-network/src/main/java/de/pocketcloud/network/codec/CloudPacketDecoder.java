package de.pocketcloud.network.codec;

import de.pocketcloud.network.traffic.PacketTrafficListener;
import de.pocketcloud.network.traffic.TrafficDirection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class CloudPacketDecoder extends ByteToMessageDecoder {

    private final BooleanSupplier encryptionEnabled;
    private final IntSupplier maxPacketSizeSupplier;
    private final Supplier<String> authTokenSupplier;
    private final PacketTrafficListener trafficListener;

    public CloudPacketDecoder(BooleanSupplier encryptionEnabled, IntSupplier maxPacketSizeSupplier, Supplier<String> authTokenSupplier, PacketTrafficListener trafficListener) {
        this.encryptionEnabled = encryptionEnabled;
        this.maxPacketSizeSupplier = maxPacketSizeSupplier;
        this.authTokenSupplier = authTokenSupplier;
        this.trafficListener = trafficListener;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (!in.isReadable()) return;

        in.markReaderIndex();
        int length = in.readInt();
        if (in.readableBytes() < length) {
            in.resetReaderIndex();
            return;
        }

        if (length > maxPacketSizeSupplier.getAsInt()) {
            trafficListener.onTooLargePacket(ctx.channel(), null, length, TrafficDirection.IN);
            return;
        }

        byte[] bytes = new byte[length];
        in.readBytes(bytes);

        if (!trafficListener.onIncoming(ctx.channel(), bytes, length)) return;

        var packet = PacketSerializer.decode(bytes, encryptionEnabled.getAsBoolean(), authTokenSupplier.get(), trafficListener::onPacketResolve);
        if (packet == null) {
            trafficListener.onUnknownPacket(ctx.channel(), bytes, length);
            return;
        }

        packet.setSize(length);
        out.add(packet);
    }
}