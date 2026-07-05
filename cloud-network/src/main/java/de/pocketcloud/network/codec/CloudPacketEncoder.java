package de.pocketcloud.network.codec;

import de.pocketcloud.network.packet.Packet;
import de.pocketcloud.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class CloudPacketEncoder extends MessageToByteEncoder<Packet> {

    private final BooleanSupplier encryptionEnabled;
    private final Supplier<String> authTokenSupplier;

    public CloudPacketEncoder(BooleanSupplier encryptionEnabled, Supplier<String> authTokenSupplier) {
        this.encryptionEnabled = encryptionEnabled;
        this.authTokenSupplier = authTokenSupplier;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf out) {
        byte[] bytes = PacketSerializer.encode(packet, encryptionEnabled.getAsBoolean(), authTokenSupplier.get());
        packet.setSize(bytes.length);
        TrafficMonitorManager.instance().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.OUT, bytes.length);
        TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, TrafficDirection.OUT, ctx.channel().remoteAddress(), new String(bytes, StandardCharsets.UTF_8), packet.getSize());
        TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, TrafficDirection.OUT, ctx.channel().remoteAddress(), packet, packet.getSize());
        out.writeInt(bytes.length);
        out.writeBytes(bytes);
    }
}