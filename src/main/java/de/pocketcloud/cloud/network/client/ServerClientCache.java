package de.pocketcloud.cloud.network.client;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.tick.Tickable;
import io.netty.channel.Channel;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class ServerClientCache implements Tickable {

    @Getter
    private static ServerClientCache instance;

    /** serverName -> ServerClient */
    private final Map<String, ServerClient> clientsByServer = new HashMap<>();
    /** client.toString() ->serverName */
    private final Map<String, String> serversByClient = new HashMap<>();
    /** SocketAddress.toString() -> ServerClient */
    private final Map<String, ServerClient> clientsByAddress = new HashMap<>();

    public ServerClientCache() {
        instance = this;
    }

    @Override
    public void tick(long currentTick) {
        for (ServerClient client : clientsByServer.values()) {
            for (ServerClient.DelayedPacket dp : client.pollDuePackets()) {
                boolean success = client.sendPacket((de.pocketcloud.cloud.network.packet.ClientboundPacket) dp.packet());
                if (dp.onSend() != null) {
                    dp.onSend().accept(client, dp.packet(), success);
                }
            }
        }
    }

//    public synchronized void add(CloudServer server, ServerClient client) {
//        if (isRegistered(client)) return;
//        CloudLogger.get().debug("Adding client {} => {}", client, server.getName());
//        clientsByServer.put(server.getName(), client);
//        serversByClient.put(client.toString(), server.getName());
//        clientsByAddress.put(client.getAddress().toString(), client);
//    }

    public synchronized void remove(ServerClient client) {
        if (!isRegistered(client)) return;
        CloudLogger.get().debug("Removing client {}", client);
        String serverName = serversByClient.remove(client.toString());
        if (serverName != null) {
            clientsByServer.remove(serverName);
        } else {
            clientsByServer.values().remove(client);
        }
        clientsByAddress.remove(client.getAddress().toString());
    }

    //TODo
//    public synchronized void remove(CloudServer server) {
//        ServerClient client = clientsByServer.get(server.getName());
//        if (client != null) remove(client);
//    }

    public synchronized boolean isRegistered(ServerClient client) {
        return serversByClient.containsKey(client.toString());
    }

    public synchronized ServerClient getByChannel(Channel channel) {
        return clientsByAddress.get(channel.remoteAddress().toString());
    }

    public synchronized ServerClient getByAddress(SocketAddress address) {
        return clientsByAddress.get(address.toString());
    }

//    public synchronized ServerClient get(CloudServer server) {
//        return clientsByServer.get(server.getName());
//    }
//
//    public synchronized CloudServer getServer(ServerClient client) {
//        String serverName = serversByClient.get(client.toString());
//        if (serverName == null) return null;
//        // Resolve through your CloudServerManager — adjust the call to match your actual API.
//        return de.pocketcloud.cloud.server.CloudServerManager.getInstance().get(serverName);
//    }

    public synchronized List<ServerClient> getAll(Predicate<ServerClient> filter) {
        return clientsByServer.values().stream()
                .filter(filter)
                .toList();
    }

    public synchronized Collection<ServerClient> getAll() {
        return List.copyOf(clientsByServer.values());
    }

    public static long convertTicksToMs(int ticks) {
        return ticks * 50L; // 1000 ms / 20 tps
    }
}
