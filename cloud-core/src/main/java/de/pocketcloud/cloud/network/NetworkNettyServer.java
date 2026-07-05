package de.pocketcloud.cloud.network;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.util.StringUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

public class NetworkNettyServer {

    private final InetSocketAddress address;
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    @Getter
    @Accessors(fluent = true)
    private final String authToken = StringUtils.generate(15);

    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory("boss-group"),
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory()
    );

    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory("worker-group"),
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory()
    );

    public NetworkNettyServer(InetSocketAddress address) {
        this.address = address;
    }

    public void start() {
        try {
            new ServerBootstrap()
                    .channelFactory(Epoll.isAvailable() ? EpollServerSocketChannel::new : NioServerSocketChannel::new)
                    .group(bossGroup, workerGroup)
                    .childHandler(new NetworkNettyServerInitializer())
                    .bind(address)
                    .addListener(_ -> Thread.currentThread().setName("Network"))
                    .sync();

            CloudLogger.get().info("§bNetwork connection §rhas been §aestablished §ron §b{}§r.", address.toString());
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to establish network connection, shutting down...", e);
            PocketCloud.instance().shutdown();
        }
    }

    public void close() {
        this.channels.close().awaitUninterruptibly();
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }

    public void addChannel(Channel channel) {
        if (channels.add(channel)) {
            CloudLogger.get().info("Client connected: {}", channel.remoteAddress());
        }
    }

    public void removeChannel(Channel channel) {
        if (channels.remove(channel)) {
            CloudLogger.get().info("Client disconnected: {}", channel.remoteAddress());
        }
    }

    public boolean isChannelActive(Channel channel) {
        return channels.find(channel.id()) != null;
    }
}