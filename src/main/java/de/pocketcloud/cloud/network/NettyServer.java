package de.pocketcloud.cloud.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;

public class NettyServer {

    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory("NettyBossGroup"),
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory()
    );

    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory("NettyWorkerGroup"),
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory()
    );

    public void start() {
        new ServerBootstrap()
                .channelFactory(Epoll.isAvailable() ? EpollServerSocketChannel::new : NioServerSocketChannel::new)
                .group(bossGroup, workerGroup)
                .childHandler(new NettyServerInitializer())
                .bind("0.0.0.0", 1913)
                .addListener(_ -> Thread.currentThread().setName("Network"));
    }

    public void close() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }
}
