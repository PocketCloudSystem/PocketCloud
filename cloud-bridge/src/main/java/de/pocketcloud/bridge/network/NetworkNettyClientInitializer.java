package de.pocketcloud.bridge.network;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.network.codec.CloudPacketDecoder;
import de.pocketcloud.network.codec.CloudPacketEncoder;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.traffic.PacketTrafficListener;
import de.pocketcloud.network.traffic.TrafficMonitorManager;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import de.pocketcloud.shared.event.network.PacketSentEvent;
import de.pocketcloud.shared.event.network.PacketTooLargeEvent;
import de.pocketcloud.shared.event.network.UnknownPacketReceivedEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.jetbrains.annotations.Nullable;

public class NetworkNettyClientInitializer extends ChannelInitializer<Channel> {

    private static final PacketTrafficListener LISTENER = new PacketTrafficListener() {

        @Override
        public Packet onPacketResolve(String packetName) {
            return CloudBridge.instance().packets().get(packetName);
        }

        @Override
        public boolean onOutgoing(Channel channel, Packet packet, byte[] payload, int length) {
            if (packet instanceof RequestPacket p) {
                CloudBridge.instance().requests().add(p);
            }

            TrafficMonitorManager.instance().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.OUT, length);
            TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, TrafficDirection.OUT, channel, payload, packet.getSize());
            TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.OUT, packet.getClass()), channel, packet, packet.getSize());
            CloudAPI.instance().events().call(new PacketSentEvent(packet, channel));
            return true;
        }

        @Override
        public boolean onIncoming(Channel channel, byte[] payload, int length) {
            TrafficMonitorManager.instance().pushBytes(NetworkTrafficMonitor.class, TrafficDirection.IN, length);
            TrafficMonitorManager.instance().callHandlers(NetworkTrafficMonitor.class, TrafficDirection.IN, channel, payload, (long) length);
            return true;
        }

        @Override
        public void onUnknownPacket(Channel channel, byte[] payload, int length) {
            CloudAPI.instance().events().call(new UnknownPacketReceivedEvent(channel, payload, length));
            CloudBridge.instance().logger().debug("Received unknown packet with size {} from {}", length, channel.remoteAddress().toString());
        }

        @Override
        public void onTooLargePacket(Channel channel, @Nullable Packet packet, int length, TrafficDirection direction) {
            CloudAPI.instance().events().call(new PacketTooLargeEvent(channel, packet, length, direction));
            if (direction.equals(TrafficDirection.IN)) {
                CloudBridge.instance().logger().debug("Received a way too big packet with size {} from {}", length, channel.remoteAddress().toString());
            } else {
                CloudBridge.instance().logger().debug("Tried to send a way too big packet with size {} to {}", length, channel.remoteAddress().toString());
            }
        }
    };

    @Override
    protected void initChannel(Channel channel) {
        boolean encryption = CloudBridge.instance().environmentConfig().networkEncryption();
        int packetLimit = CloudBridge.instance().environmentConfig().networkPacketSizeLimit();
        String authKey = CloudBridge.instance().environmentConfig().networkAuthKey();

        channel.pipeline().addLast(
                new CloudPacketDecoder(() -> encryption, () -> packetLimit, () -> authKey, LISTENER),
                new CloudPacketEncoder(() -> encryption, () -> packetLimit, () -> authKey, LISTENER),
                new NetworkNettyHandler()
        );
    }
}