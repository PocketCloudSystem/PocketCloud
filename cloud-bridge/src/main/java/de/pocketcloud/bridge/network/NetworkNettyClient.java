package de.pocketcloud.bridge.network;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import dev.waterdog.waterdogpe.ProxyServer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

@Getter
public final class NetworkNettyClient {

    private final SocketAddress address;
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory("worker-group"),
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory()
    );

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

            CloudAPI.instance().logger().info("§bNetwork connection §rhas been §aestablished §rtowards §b{}§r.", address.toString());
        } catch (Exception e) {
            CloudAPI.instance().logger().error("Failed to establish network connection, shutting down...", e);
            ProxyServer.getInstance().shutdown();
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