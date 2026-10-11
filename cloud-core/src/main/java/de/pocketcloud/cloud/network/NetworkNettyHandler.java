package de.pocketcloud.cloud.network;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.packet.PacketReceiveEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.log.LogThrottle;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import de.pocketcloud.shared.event.network.PacketReceivedEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class NetworkNettyHandler extends SimpleChannelInboundHandler<CloudPacket> {

    private static final int MAX_STRIKES = 20;
    private static final AttributeKey<AtomicInteger> STRIKES = AttributeKey.valueOf("pc_strikes");
    private static final LogThrottle ERROR_THROTTLE = new LogThrottle(3, Duration.ofSeconds(10));
    private static final AttributeKey<AtomicInteger> PENDING = AttributeKey.valueOf("pc_pending");
    private static final int MAX_PENDING_UNAUTHENTICATED = 20;
    private static final int MAX_PENDING_AUTHENTICATED = 2000;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        PocketCloud.instance().network().addChannel(channel);
        channel.attr(ServerClient.ATTRIBUTE_KEY).set(new ServerClient(channel));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
        try {
            PocketCloud.instance().traffic().callHandlers(NetworkTrafficMonitor.class, NetworkTrafficMonitor.parsePacketMode(TrafficDirection.IN, packet.getClass()), ctx.channel(), packet, packet.getSize());
        } catch (Exception e) {
            logThrottled("traffic:" + e.getClass().getSimpleName(), "Unhandled exception while handling traffic sent by §b" + ctx.channel().remoteAddress() + "§r.", e);
        }

        if (!PocketCloud.instance().running()) {
            process(ctx, packet);
            return;
        }

        Channel channel = ctx.channel();
        ServerClient c = channel.attr(ServerClient.ATTRIBUTE_KEY).get();
        boolean authenticated = c != null && c.server() != null;
        int limit = authenticated ? MAX_PENDING_AUTHENTICATED : MAX_PENDING_UNAUTHENTICATED;

        AtomicInteger pending = channel.attr(PENDING).setIfAbsent(new AtomicInteger());
        if (pending == null) pending = channel.attr(PENDING).get();

        if (pending.incrementAndGet() > limit) {
            pending.decrementAndGet();
            logThrottled("flood", "Closing connection §b" + channel.remoteAddress() + "§r: too many pending packets.", new IllegalStateException("packet flood"));
            ctx.close();
            return;
        }

        AtomicInteger counter = pending;
        PocketCloud.instance().packetQueue().submit(() -> {
            try {
                process(ctx, packet);
            } finally {
                counter.decrementAndGet();
            }
        });
    }

    private void process(ChannelHandlerContext ctx, CloudPacket packet) {
        ServerClient c = ctx.channel().attr(ServerClient.ATTRIBUTE_KEY).get();
        if (c == null) return;

        try {
            CloudServer server = null;

            if (packet instanceof AuthenticatedPacket) {
                if (c.server() == null) {
                    registerStrike(ctx);
                    return;
                }
                server = c.server();
            }

            if (new PacketReceiveEvent(ctx.channel(), (CloudboundPacket) packet).call().isCancelled()) return;

            CloudAPI.instance().events().call(new PacketReceivedEvent(packet, ctx.channel()));

            if (packet instanceof ResponsePacket responsePacket) {
                PocketCloud.instance().requests().resolve(responsePacket);
            }

            PocketCloud.instance().packets().invokeHandlers(packet, c);

            if (server != null) server.latestPacketInfo().setLatestPacket(Instant.now(), packet.getClass());
        } catch (Exception e) {
            logThrottled(
                    packet.getClass().getSimpleName() + ":" + e.getClass().getSimpleName(),
                    "Unhandled exception while processing packet §b" + packet.getName() + " §rsent by §b" + ctx.channel().remoteAddress() + "§r.",
                    e
            );

            if (c.server() == null) registerStrike(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            ctx.close();
            return;
        }

        logThrottled(
                "decoder:" + cause.getClass().getSimpleName(),
                "Unhandled exception caused by §b" + ctx.channel().remoteAddress() + "§r.",
                cause
        );

        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();

        if (!PocketCloud.instance().running()) {
            disconnectChannel(channel);
            return;
        }

        PocketCloud.instance().packetQueue().submit(() -> disconnectChannel(channel));
    }

    private void disconnectChannel(Channel channel) {
        PocketCloud.instance().network().removeChannel(channel);
        ServerClient client = channel.attr(ServerClient.ATTRIBUTE_KEY).get();
        if (client != null) {
            PocketCloud.instance().clients().remove(client);
        }
    }

    private void registerStrike(ChannelHandlerContext ctx) {
        AtomicInteger counter = ctx.channel().attr(STRIKES).setIfAbsent(new AtomicInteger());
        if (counter == null) counter = ctx.channel().attr(STRIKES).get();

        if (counter.incrementAndGet() == MAX_STRIKES) {
            CloudLogger.get().warn("Closing connection §b{}§r: too many invalid packets.", ctx.channel().remoteAddress());
            ctx.close();
        }
    }

    private void logThrottled(String key, String message, Throwable t) {
        int suppressed = ERROR_THROTTLE.acquire(key);
        if (suppressed < 0) return;

        if (suppressed > 0) {
            CloudLogger.get().warn("§8({} similar errors were suppressed)", suppressed);
        }

        boolean debug = CloudLogger.get().isDebugMode();
        CloudLogger.get().error(message + (debug ? "" : " §8(§renable §edebug §rto view full stack trace§8)"));
        if (debug) CloudLogger.get().exception(t);
        else CloudLogger.get().error(String.valueOf(t.getMessage()));
    }
}