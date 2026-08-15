package de.pocketcloud.bridge.network;

import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.bridge.CloudBridge;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Getter
public final class NetworkNettyClient {

    private final SocketAddress address;
    private final EventLoopGroup workerGroup = Epoll.isAvailable()
            ? new EpollEventLoopGroup(new DefaultThreadFactory("worker-group"))
            : new NioEventLoopGroup(new DefaultThreadFactory("worker-group"));

    private final Set<CompletableFuture<Void>> pendingWrites = ConcurrentHashMap.newKeySet();

    private Channel channel;

    public NetworkNettyClient(SocketAddress address) {
        this.address = address;
    }

    public void start() throws InterruptedException {
        channel = new Bootstrap()
                .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
                .group(workerGroup)
                .handler(new NetworkNettyClientInitializer())
                .connect(address)
                .addListener(_ -> Thread.currentThread().setName("Network"))
                .sync().channel();

        CloudBridge.instance().logger().info("§bNetwork connection §rhas been §aestablished §rtowards §b{}§r.", address.toString());
    }

    public CompletableFuture<Void> sendPacket(CloudboundPacket packet) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        pendingWrites.add(future);

        channel.writeAndFlush(packet).addListener(f -> {
            if (f.isSuccess()) {
                future.complete(null);
            } else {
                future.completeExceptionally(f.cause());
            }
            pendingWrites.remove(future);
        });

        return future;
    }

    public void close() {
        try {
            CompletableFuture.allOf(pendingWrites.toArray(new CompletableFuture[0])).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            CloudBridge.instance().logger().warn("§cNot all packets could be flushed before shutdown: {}", e.getMessage());
        }

        if (channel != null) {
            channel.close().syncUninterruptibly();
        }

        workerGroup.shutdownGracefully();
    }
}