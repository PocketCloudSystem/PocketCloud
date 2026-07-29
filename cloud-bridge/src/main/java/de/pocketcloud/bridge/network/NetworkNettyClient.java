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
import java.util.concurrent.CompletableFuture;

@Getter
public final class NetworkNettyClient {

    private final SocketAddress address;
    private final EventLoopGroup workerGroup = Epoll.isAvailable()
            ? new EpollEventLoopGroup(new DefaultThreadFactory("worker-group"))
            : new NioEventLoopGroup(new DefaultThreadFactory("worker-group"));

    private Channel channel;

    public NetworkNettyClient(SocketAddress address) {
        this.address = address;
    }

    public void start() {
        try {
            channel = new Bootstrap()
                    .channel(Epoll.isAvailable() ? EpollSocketChannel.class : NioSocketChannel.class)
                    .group(workerGroup)
                    .handler(new NetworkNettyClientInitializer())
                    .connect(address)
                    .addListener(_ -> Thread.currentThread().setName("Network"))
                    .sync().channel();

            CloudBridge.instance().logger().info("§bNetwork connection §rhas been §aestablished §rtowards §b{}§r.", address.toString());
        } catch (Exception e) {
            CloudBridge.instance().logger().error("Failed to establish network connection, shutting down...", e);
            CloudBridge.instance().shutdown();
        }
    }

    public CompletableFuture<Void> sendPacket(CloudboundPacket packet) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        channel.writeAndFlush(packet).addListener(f -> {
            if (f.isSuccess()) {
                future.complete(null);
            } else {
                future.completeExceptionally(f.cause());
            }
        });

        return future;
    }

    public void close() {
        this.workerGroup.shutdownGracefully();
    }
}