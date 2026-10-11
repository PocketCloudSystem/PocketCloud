package de.pocketcloud.network.codec;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.PacketTrafficListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class CloudPacketEncoder extends MessageToByteEncoder<Packet> {

    private final BooleanSupplier encryptionEnabled;
    private final IntSupplier maxPacketSizeSupplier;
    private final Supplier<String> authTokenSupplier;
    private final PacketTrafficListener trafficListener;

    public CloudPacketEncoder(BooleanSupplier encryptionEnabled, IntSupplier maxPacketSizeSupplier, Supplier<String> authTokenSupplier, PacketTrafficListener trafficListener) {
        this.encryptionEnabled = encryptionEnabled;
        this.maxPacketSizeSupplier = maxPacketSizeSupplier;
        this.authTokenSupplier = authTokenSupplier;
        this.trafficListener = trafficListener;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) throws Exception {
        byte[] bytes = PacketSerializer.encode(packet, encryptionEnabled.getAsBoolean(), authTokenSupplier.get());
        int maxSize = maxPacketSizeSupplier.getAsInt();

        if (bytes.length > maxSize) {
            trafficListener.onTooLargePacket(ctx.channel(), packet, bytes.length, TrafficDirection.OUT);
            throw new IllegalArgumentException("Packet " + packet.getClass().getSimpleName() + " is too large (" + bytes.length + " bytes > max " + maxSize + ")");
        }

        packet.setSize(bytes.length);
        if (!trafficListener.onOutgoing(ctx.channel(), packet, bytes, bytes.length)) {
            throw new IllegalStateException("Packet " + packet.getClass().getSimpleName() + " transmission was cancelled by traffic listener");
        }

        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}