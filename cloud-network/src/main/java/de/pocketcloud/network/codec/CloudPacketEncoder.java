package de.pocketcloud.network.codec;

import de.pocketcloud.network.packet.Packet;
import de.pocketcloud.network.traffic.PacketTrafficListener;
import de.pocketcloud.network.traffic.TrafficDirection;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;
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
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
        byte[] bytes = PacketSerializer.encode(packet, encryptionEnabled.getAsBoolean(), authTokenSupplier.get());
        if (bytes.length > maxPacketSizeSupplier.getAsInt()) {
            trafficListener.onTooLargePacket(ctx.channel(), packet, bytes.length, TrafficDirection.OUT);
            return;
        }

        packet.setSize(bytes.length);
        if (!trafficListener.onOutgoing(ctx.channel(), packet, bytes, bytes.length)) return;
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}