package de.pocketcloud.cloud.network.broadcaster;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.MainConfig;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.util.FilterableObject;
import de.pocketcloud.network.codec.PacketSerializer;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PacketBroadcaster {

    public static void broadcastPackets(ClientboundPacket[] packets, FilterableObject... exclusions) {
        if (packets.length == 0) return;
        List<Map.Entry<ClientboundPacket, ByteBuf>> encodedPackets = new ArrayList<>();
        long bytes = 0;
        long targets = 0;
        Set<FilterableObject> exclusionSet = Set.of(exclusions);

        for (ClientboundPacket packet : packets) {
            byte[] packetBuffer = PacketSerializer.encode(packet, MainConfig.instance().isNetworkEncryptionEnabled(), PocketCloud.instance().network().authToken());
            int packetLength = packetBuffer.length;
            if (packet instanceof CloudPacket cloudPacket) cloudPacket.setSize(packetLength);
            ByteBuf buffer = Unpooled.buffer();
            buffer.writeInt(packetLength);
            buffer.writeBytes(packetBuffer);
            encodedPackets.add(Map.entry(packet, buffer));
            bytes += 4 + packetLength;
        }

        for (ServerClient client : ServerClientCache.instance().getAll()) {
            if (!client.hasServer()) continue;
            CloudServer server = client.server();
            Template template = server.template();
            if (exclusionSet.contains(client) ||
                    exclusionSet.contains(server) ||
                    exclusionSet.contains(template) ||
                    exclusionSet.contains(template.templateType())) continue;

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

        TrafficMonitorManager.instance().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.OUT, bytes * targets);
    }

    public static void broadcastPacket(ClientboundPacket packet, FilterableObject... exclusions) {
        broadcastPackets(new ClientboundPacket[]{packet}, exclusions);
    }
}