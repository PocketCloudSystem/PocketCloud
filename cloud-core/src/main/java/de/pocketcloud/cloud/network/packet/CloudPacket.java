package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AbstractPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.util.FilterableObject;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class CloudPacket extends AbstractPacket {

    @Override
    public final void handle(@NotNull Channel channel) {
        handle(channel.attr(ServerClient.ATTRIBUTE_KEY).get());
    }

    public abstract void handle(@NotNull ServerClient client);

    public CompletableFuture<Void> sendPacket(ServerClient client) {
        if (this instanceof ClientboundPacket p) return client.sendPacket(p);
        return CompletableFuture.failedFuture(new RuntimeException("Packet not a ClientboundPacket"));
    }

    public void broadcastPacket(FilterableObject... exclusions) {
        if (!(this instanceof ClientboundPacket p)) throw new IllegalStateException("...");
        PacketBroadcaster.broadcastPacket(p, exclusions);
    }
}