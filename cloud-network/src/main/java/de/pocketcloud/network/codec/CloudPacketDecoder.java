package de.pocketcloud.network.codec;

import de.pocketcloud.common.function.TriConsumer;
import de.pocketcloud.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class CloudPacketDecoder extends ByteToMessageDecoder {

    private final BooleanSupplier encryptionEnabled;
    private final Supplier<String> authTokenSupplier;
    private final TriConsumer<SocketAddress, String, byte[]> unknownPacketListener;

    public CloudPacketDecoder(BooleanSupplier encryptionEnabled, Supplier<String> authTokenSupplier, TriConsumer<SocketAddress, String, byte[]> unknownPacketListener) {
        this.encryptionEnabled = encryptionEnabled;
        this.authTokenSupplier = authTokenSupplier;
        this.unknownPacketListener = unknownPacketListener;
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

        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        String payload = new String(bytes, StandardCharsets.UTF_8);

        TrafficMonitorManager.instance().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.IN, length);
        TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, TrafficDirection.IN, ctx.channel().remoteAddress(), payload, (long) length);

        var packet = PacketSerializer.decode(bytes, encryptionEnabled.getAsBoolean(), authTokenSupplier.get());
        if (packet == null) {
            unknownPacketListener.accept(ctx.channel().remoteAddress(), payload, bytes);
            return;
        }

        packet.setSize(length);
        out.add(packet);
    }
}