package de.pocketcloud.cloud.network;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.packet.PacketPreSendEvent;
import de.pocketcloud.cloud.event.impl.packet.PacketReceivePreProcessEvent;
import de.pocketcloud.network.codec.CloudPacketDecoder;
import de.pocketcloud.network.codec.CloudPacketEncoder;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.traffic.PacketTrafficListener;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import de.pocketcloud.shared.event.network.PacketSentEvent;
import de.pocketcloud.shared.event.network.PacketTooLargeEvent;
import de.pocketcloud.shared.event.network.UnknownPacketReceivedEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.Nullable;

public class NetworkNettyServerInitializer extends ChannelInitializer<Channel> {

    private static final PacketTrafficListener LISTENER = new PacketTrafficListener() {

        @Override
        public Packet onPacketResolve(String packetName) {
            return PocketCloud.instance().packets().get(packetName);
        }

        @Override
        public boolean onOutgoing(Channel channel, Packet packet, byte[] payload, int length) {
            PocketCloud.instance().traffic().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.OUT, length);
            PocketCloud.instance().traffic().callHandlers(NetworkTrafficMonitor.class, TrafficDirection.OUT, channel, payload, packet.getSize());

            if (new PacketPreSendEvent(channel, (ClientboundPacket) packet).call().isCancelled()) {
                return false;
            }

            if (packet instanceof RequestPacket p) {
                PocketCloud.instance().requests().add(p);
            }

            PocketCloud.instance().traffic().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.OUT, packet.getClass()), channel, packet, packet.getSize());
            CloudAPI.instance().events().call(new PacketSentEvent(packet, channel));

            return true;
        }

        @Override
        public boolean onIncoming(Channel channel, byte[] payload, int length) {
            PocketCloud.instance().traffic().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.IN, length);
            PocketCloud.instance().traffic().callHandlers(NetworkTrafficMonitor.class, TrafficDirection.IN, channel, payload, (long) length);

            return !new PacketReceivePreProcessEvent(channel, payload, PocketCloud.instance().config().network().encryption()).call().isCancelled();
        }

        @Override
        public void onUnknownPacket(Channel channel, byte[] payload, int length) {
            CloudAPI.instance().events().call(new UnknownPacketReceivedEvent(channel, payload, length));
            CloudLogger.get().debug("Received unknown packet with size {} from {}", length, channel.remoteAddress().toString());
        }

        @Override
        public void onTooLargePacket(Channel channel, @Nullable Packet packet, int length, TrafficDirection direction) {
            CloudAPI.instance().events().call(new PacketTooLargeEvent(channel, packet, length, direction));
            if (direction.equals(TrafficDirection.IN)) {
                CloudLogger.get().debug("Received a way too big packet with size {} from {}", length, channel.remoteAddress().toString());
            } else {
                CloudLogger.get().debug("Tried to send a way too big packet with size {} to {}", length, channel.remoteAddress().toString());
            }
        }
    };

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(
            new CloudPacketDecoder(() -> PocketCloud.instance().config().network().encryption(), () -> Math.toIntExact(PocketCloud.instance().config().network().packetSizeLimit()), () -> PocketCloud.instance().network().authToken(), LISTENER),
            new CloudPacketEncoder(() -> PocketCloud.instance().config().network().encryption(), () -> Math.toIntExact(PocketCloud.instance().config().network().packetSizeLimit()), () -> PocketCloud.instance().network().authToken(), LISTENER),
            new NetworkNettyHandler()
        );
    }
}