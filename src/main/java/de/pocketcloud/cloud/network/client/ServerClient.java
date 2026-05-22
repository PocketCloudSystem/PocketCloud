package de.pocketcloud.cloud.network.client;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.util.TriConsumer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.AttributeKey;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public final class ServerClient {

    /** Netty channel attribute used to attach a {@link ServerClient} directly to its channel. */
    public static final AttributeKey<ServerClient> ATTRIBUTE_KEY = AttributeKey.valueOf("serverClient");

    public record DelayedPacket(CloudPacket packet, long deliverAt, TriConsumer<ServerClient, CloudPacket, Boolean> onSend) {}

    @Getter
    private final Channel channel;
    private final List<DelayedPacket> delayedPackets = new ArrayList<>();

    public ServerClient(Channel channel) {
        this.channel = channel;
    }

    public boolean sendPacket(ClientboundPacket packet) {
        if (!channel.isActive()) {
            CloudLogger.get().warn("Failed to send packet §b{} §rto §b{}§r.", ((CloudPacket) packet).getName(), channel.remoteAddress());
            return false;
        }

        channel.writeAndFlush(packet).addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                CloudLogger.get().warn("Failed to send packet §b{} §rto §b{}§r.", ((CloudPacket) packet).getName(), channel.remoteAddress());
            }
        });
        return true;
    }

    public void sendDelayedPacket(CloudPacket packet, long delayMs, TriConsumer<ServerClient, CloudPacket, Boolean> onSend) {
        delayedPackets.add(new DelayedPacket(packet, System.currentTimeMillis() + delayMs, onSend));
    }

    public List<DelayedPacket> pollDuePackets() {
        long now = System.currentTimeMillis();
        List<DelayedPacket> due = delayedPackets.stream()
                .filter(dp -> dp.deliverAt() <= now)
                .toList();
        delayedPackets.removeAll(due);
        return due;
    }

    public List<DelayedPacket> getDelayedPackets() {
        return List.copyOf(delayedPackets);
    }

    public SocketAddress getAddress() {
        return channel.remoteAddress();
    }

//    //todo
//    public boolean hasServer() {
//        return getServer() != null;
//    }

    //Todo
//    public CloudServer getServer() {
//        return ServerClientCache.getInstance().getServer(this);
//    }

    @Override
    public String toString() {
        return "ServerClient[address=" + getAddress() + "]";
    }
}
