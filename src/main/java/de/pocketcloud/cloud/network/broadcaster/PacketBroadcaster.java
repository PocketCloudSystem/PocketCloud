package de.pocketcloud.cloud.network.broadcaster;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.impl.MainConfig;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.codec.PacketSerializer;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.traffic.TrafficMonitor;
import de.pocketcloud.cloud.traffic.TrafficMonitorManager;
import de.pocketcloud.cloud.util.FilterableObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PacketBroadcaster {

    public static void broadcastPackets(ClientboundPacket[] packets, FilterableObject... exclusions) {
        if (packets.length == 0) return;
        List<byte[]> encodedPackets = new ArrayList<>();
        long bytes = 0;
        long targets = 0;
        Set<FilterableObject> exclusionSet = Set.of(exclusions);

        for (ClientboundPacket packet : packets) {
            byte[] buffer = PacketSerializer.encode(
                    packet,
                    MainConfig.instance().isNetworkEncryptionEnabled(),
                    PocketCloud.instance().network().authToken()
            );

            encodedPackets.add(buffer);
            bytes += buffer.length;
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
            for (byte[] packet : encodedPackets) {
                client.channel().write(packet);
            }

            client.channel().flush();
        }

        TrafficMonitorManager.instance().pushBytes(TrafficMonitorManager.TRAFFIC_NETWORK, bytes * targets, TrafficMonitor.REGULAR_MODE_OUT);
    }

    public static void broadcastPacket(ClientboundPacket packet, FilterableObject... exclusions) {
        broadcastPackets(new ClientboundPacket[]{packet}, exclusions);
    }
}