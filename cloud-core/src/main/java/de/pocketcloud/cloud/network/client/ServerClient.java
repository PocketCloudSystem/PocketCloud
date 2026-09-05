package de.pocketcloud.cloud.network.client;

import de.pocketcloud.api.network.client.IServerClient;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public final class ServerClient implements IServerClient {

    public static final AttributeKey<ServerClient> ATTRIBUTE_KEY = AttributeKey.valueOf("serverClient");

    public record DelayedPacket(ClientboundPacket packet, long deliverAt, CompletableFuture<Void> future) {}

    @Getter
    @Accessors(fluent = true)
    private final Channel channel;
    private final Queue<DelayedPacket> delayedPackets = new ConcurrentLinkedQueue<>();

    public ServerClient(Channel channel) {
        this.channel = channel;
    }

    public CompletableFuture<Void> sendPacket(ClientboundPacket packet) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        channel.writeAndFlush(packet).addListener(f -> {
            if (f.isSuccess()) {
                future.complete(null);
            } else {
                future.completeExceptionally(f.cause());
            }
        });

        return future;
    }

    @Override
    public CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long delay, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        delayedPackets.add(new DelayedPacket(packet, System.currentTimeMillis() + unit.toMillis(delay), future));
        return future;
    }

    @Override
    public CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long ticks) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        delayedPackets.add(new DelayedPacket(packet, System.currentTimeMillis() + (ticks * 50), future));
        return future;
    }

    public List<DelayedPacket> pollDuePackets() {
        long now = System.currentTimeMillis();
        List<DelayedPacket> due = new ArrayList<>();
        delayedPackets.removeIf(dp -> {
            if (dp.deliverAt() <= now) {
                due.add(dp);
                return true;
            }
            return false;
        });

        return due;
    }

    public List<DelayedPacket> delayedPackets() {
        return List.copyOf(delayedPackets);
    }

    public SocketAddress address() {
        return channel.remoteAddress();
    }

    public boolean hasServer() {
        return server() != null;
    }

    public CloudServer server() {
        return (CloudServer) PocketCloud.instance().clients().getServer(this).orElse(null);
    }

    @Override
    public String toString() {
        return "ServerClient[address=" + address() + "]";
    }
}