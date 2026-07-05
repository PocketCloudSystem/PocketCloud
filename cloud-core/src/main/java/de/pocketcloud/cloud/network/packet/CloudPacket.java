package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.Packet;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.util.FilterableObject;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public abstract class CloudPacket implements Packet {
    
    private boolean encoded = false;
    private Long sentTimestamp = null;
    @Setter
    @Getter
    private long size = 0;
    
    @Override
    public void encode(PacketData packetData) {
        if (encoded) throw new RuntimeException("Packet " + getName() + " has already been encoded");
        encoded = true;
        packetData.write(getName()).write(sentTimestamp = System.currentTimeMillis());
        encodePayload(packetData);
    }
    
    @Override
    public void decode(PacketData packetData) {
        String packetName = packetData.readString();
        assert packetName != null;
        if (!packetName.equals(getName())) throw new RuntimeException("Packet name does not equal the actual class name? What have you done?");
        sentTimestamp = packetData.readLong();
        decodePayload(packetData);
    }

    public CompletableFuture<Void> sendPacket(ServerClient client) {
        if (this instanceof ClientboundPacket p) return client.sendPacket(p);
        return CompletableFuture.failedFuture(new RuntimeException("Packet not a ClientboundPacket"));
    }

    public void broadcastPacket(FilterableObject... exclusions) {
        if (!(this instanceof ClientboundPacket p)) throw new IllegalStateException("Cannot broadcast non-ClientboundPacket");
        PacketBroadcaster.broadcastPacket(p, exclusions);
    }

    @Override
    public void handle(@NotNull Channel channel) {
        handle(channel.attr(ServerClient.ATTRIBUTE_KEY).get());
    }

    public abstract void handle(@NotNull ServerClient client);

    @Override
    public final String getName() {
        return getClass().getSimpleName();
    }
    
    @Override
    public final boolean isEncoded() {
        return encoded;
    }
    
    @Override
    public final long getSentTimestamp() {
        return sentTimestamp;
    }
}