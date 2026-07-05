package de.pocketcloud.cloud.network.client;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.cloud.network.packet.impl.KeepAlivePacket;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.cloud.util.PerformanceStats;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.SocketAddress;
import java.util.*;
import java.util.function.Predicate;

public final class ServerClientCache implements Tickable {

    @Getter
    @Accessors(fluent = true)
    private static ServerClientCache instance = null;

    private final Map<UUID, ServerClient> clientsByServer = new HashMap<>();
    private final Map<String, UUID> serversByClient = new HashMap<>();
    private final Map<String, ServerClient> clientsByAddress = new HashMap<>();

    public ServerClientCache() {
        instance = this;
    }

    @Override
    public void tick(long currentTick) {
        if (currentTick % 30 == 0) {
            PerformanceStats stats = PocketCloud.instance().performanceStats();
            PacketBroadcaster.broadcastPacket(KeepAlivePacket.create(
                    stats.currentTPS(),
                    stats.averageTPS(),
                    stats.processUsedMemory(),
                    stats.processPeakUsedMemory(),
                    stats.processMaxMemory(),
                    stats.processCpuUsage()
            ));
        }

        for (ServerClient client : clientsByServer.values()) {
            for (ServerClient.DelayedPacket dp : client.pollDuePackets()) {
                client.sendPacket(dp.packet())
                        .thenAccept(dp.future()::complete)
                        .exceptionally(e -> {
                            dp.future().completeExceptionally(e);
                            return null;
                        });
            }
        }
    }

    public synchronized void add(CloudServer server, ServerClient client) {
        if (isRegistered(client)) return;
        CloudLogger.get().debug("Adding client {} => {} ({})", client, server.name(), server.uuid().toString());
        clientsByServer.put(server.uuid(), client);
        serversByClient.put(client.toString(), server.uuid());
        clientsByAddress.put(client.address().toString(), client);
    }

    public synchronized void remove(ServerClient client) {
        if (!isRegistered(client)) return;
        CloudLogger.get().debug("Removing client {}", client);
        UUID serverUuid = serversByClient.remove(client.toString());
        if (serverUuid != null) {
            clientsByServer.remove(serverUuid);
        } else {
            clientsByServer.values().remove(client);
        }

        clientsByAddress.remove(client.address().toString());
    }

    public synchronized void remove(CloudServer server) {
        ServerClient client = clientsByServer.get(server.uuid());
        if (client != null) remove(client);
    }

    public synchronized boolean isRegistered(ServerClient client) {
        return serversByClient.containsKey(client.toString());
    }

    public synchronized Optional<ServerClient> getByChannel(Channel channel) {
        return Optional.ofNullable(clientsByAddress.getOrDefault(channel.remoteAddress().toString(), null));
    }

    public synchronized Optional<ServerClient> getByAddress(SocketAddress address) {
        return Optional.ofNullable(clientsByAddress.getOrDefault(address.toString(), null));
    }

    public synchronized Optional<ServerClient> get(CloudServer server) {
        return Optional.ofNullable(clientsByServer.getOrDefault(server.uuid(), null));
    }

    public synchronized CloudServer getServer(ServerClient client) {
        UUID serverUuid = serversByClient.get(client.toString());
        if (serverUuid == null) return null;
        return CloudServerManager.instance().get(serverUuid).orElse(null);
    }

    public synchronized List<ServerClient> getAll(Predicate<ServerClient> filter) {
        return clientsByServer.values().stream()
                .filter(filter)
                .toList();
    }

    public synchronized Collection<ServerClient> getAll() {
        return List.copyOf(clientsByServer.values());
    }
}