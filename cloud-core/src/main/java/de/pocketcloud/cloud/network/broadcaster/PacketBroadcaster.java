package de.pocketcloud.cloud.network.broadcaster;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.codec.PacketSerializer;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import de.pocketcloud.shared.network.packet.PacketExcluder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PacketBroadcaster {

    public static void broadcast(ClientboundPacket[] packets, Consumer<PacketExcluder> excluderBuilder) {
        if (packets.length == 0) return;
        List<Map.Entry<ClientboundPacket, ByteBuf>> encodedPackets = new ArrayList<>();
        long bytes = 0;
        long targets = 0;
        PacketExcluder excluder = PacketExcluder.create();
        if (excluderBuilder != null) excluderBuilder.accept(excluder);

        for (ClientboundPacket packet : packets) {
            byte[] packetBuffer = PacketSerializer.encode(packet, PocketCloud.instance().config().network().encryption(), PocketCloud.instance().network().authToken());
            int packetLength = packetBuffer.length;
            if (packet instanceof CloudPacket cloudPacket) cloudPacket.setSize(packetLength);
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(packetLength);
            buffer.writeBytes(packetBuffer);
            encodedPackets.add(Map.entry(packet, buffer));
            bytes += 4 + packetLength;
        }

        for (ServerClient client : PocketCloud.instance().clients().getAll()) {
            if (!client.hasServer()) continue;
            if (excluder.shouldExclude(client)) continue;
            targets++;
            for (Map.Entry<ClientboundPacket, ByteBuf> entry : encodedPackets) {
                ByteBuf buffer = entry.getValue();
                client.channel().write(buffer.retainedDuplicate());
            }

            client.channel().flush();
        }

        for (Map.Entry<ClientboundPacket, ByteBuf> entry : encodedPackets) {
            entry.getValue().release();
        }

        PocketCloud.instance().traffic().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.OUT, bytes * targets);
    }

    public static void broadcast(ClientboundPacket packet, Consumer<PacketExcluder> excluderBuilder) {
        broadcast(new ClientboundPacket[]{packet}, excluderBuilder);
    }

    public static void broadcast(ClientboundPacket packet) {
        broadcast(new ClientboundPacket[]{packet}, null);
    }
}