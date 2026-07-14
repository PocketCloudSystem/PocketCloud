package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AbstractPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

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

    public void broadcastPacket() {
        broadcastPacket(null);
    }

    public void broadcastPacket(Consumer<PacketExcluder> excluderBuilder) {
        if (!(this instanceof ClientboundPacket p)) throw new IllegalStateException("Packet not a ClientboundPacket");
        PacketBroadcaster.broadcastPacket(p, excluderBuilder);
    }
}