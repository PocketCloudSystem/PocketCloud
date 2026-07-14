package de.pocketcloud.network.packet;

import de.pocketcloud.api.network.client.IServerClient;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.api.network.packet.PacketExcluder;
import de.pocketcloud.network.packet.broadcast.InternalPacketBroadcaster;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class CloudPacket implements Packet {

    @Getter
    private boolean encoded = false;
    @Getter
    private Long sentTimestamp = null;
    @Setter
    @Getter
    private long size = 0;

    @Override
    public void encode(IPacketData packetData) {
        if (encoded) throw new RuntimeException("Packet " + getName() + " has already been encoded");
        encoded = true;
        packetData.write(getName()).write(sentTimestamp = System.currentTimeMillis());
        encodePayload(packetData);
    }

    @Override
    public void decode(IPacketData packetData) {
        String packetName = packetData.readString();
        assert packetName != null;
        if (!packetName.equals(getName())) throw new RuntimeException("...");
        sentTimestamp = packetData.readLong();
        decodePayload(packetData);
    }

    /**
     * Only for cloud-bridge
     */
    public void sendPacket() {
        if (!(this instanceof CloudboundPacket)) throw new IllegalStateException("Packet not a CloudboundPacket");
        InternalPacketBroadcaster.broadcast(new Packet[]{this}, null);
        /**
         * The bridge has to set the consumer
         * {@link InternalPacketBroadcaster#setBroadcasterHandler(BiConsumer)}
         */
    }

    /**
     * Only for cloud-core
     */
    public CompletableFuture<Void> sendPacket(IServerClient client) {
        if (this instanceof ClientboundPacket p) return client.sendPacket(p);
        return CompletableFuture.failedFuture(new RuntimeException("Packet not a ClientboundPacket"));
    }

    public CompletableFuture<Void> sendDelayedPacket(IServerClient client, long delay, TimeUnit unit) {
        if (this instanceof ClientboundPacket p) return client.sendDelayedPacket(p, delay, unit);
        return CompletableFuture.failedFuture(new RuntimeException("Packet not a ClientboundPacket"));
    }

    public CompletableFuture<Void> sendDelayedPacket(IServerClient client, long ticks) {
        if (this instanceof ClientboundPacket p) return client.sendDelayedPacket(p, ticks);
        return CompletableFuture.failedFuture(new RuntimeException("Packet not a ClientboundPacket"));
    }

    public void broadcast() {
        broadcast(null);
    }

    public void broadcast(Consumer<PacketExcluder> excluderBuilder) {
        if (!(this instanceof ClientboundPacket)) throw new IllegalStateException("Packet not a ClientboundPacket");
        PacketExcluder excluder = PacketExcluder.create();
        if (excluderBuilder != null) excluderBuilder.accept(excluder);
        InternalPacketBroadcaster.broadcast(new Packet[]{this}, excluder);
    }

    @Override
    public final String getName() {
        return getClass().getSimpleName();
    }
}