package de.pocketcloud.cloud.network.client;

import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.util.FilterableObject;
import io.netty.channel.Channel;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class ServerClient implements FilterableObject {

    public static final AttributeKey<ServerClient> ATTRIBUTE_KEY = AttributeKey.valueOf("serverClient");

    public record DelayedPacket(ClientboundPacket packet, long deliverAt, CompletableFuture<Void> future) {}

    @Getter
    @Accessors(fluent = true)
    private final Channel channel;
    private final List<DelayedPacket> delayedPackets = new ArrayList<>();

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

    public CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long delayMs) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        delayedPackets.add(new DelayedPacket(packet, System.currentTimeMillis() + delayMs, future));
        return future;
    }

    public List<DelayedPacket> pollDuePackets() {
        long now = System.currentTimeMillis();
        List<DelayedPacket> due = delayedPackets.stream()
                .filter(dp -> dp.deliverAt() <= now)
                .toList();
        delayedPackets.removeAll(due);
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
        return ServerClientCache.instance().getServer(this);
    }

    @Override
    public String toString() {
        return "ServerClient[address=" + address() + "]";
    }
}