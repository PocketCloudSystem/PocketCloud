package de.pocketcloud.api.network.client;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import io.netty.channel.Channel;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface IServerClient {

    CompletableFuture<Void> sendPacket(ClientboundPacket packet);

    CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long delay, TimeUnit unit);

    CompletableFuture<Void> sendDelayedPacket(ClientboundPacket packet, long ticks);

    default boolean hasServer() {
        return server() != null;
    }

    Channel channel();

    ICloudServer server();

    SocketAddress address();
}