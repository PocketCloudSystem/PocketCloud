package de.pocketcloud.cloud.http;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.http.handler.RouterInboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;

public final class HttpServer {

    private final Router router = new Router();

    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("HttpBossGroup"), NioIoHandler.newFactory());
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("HttpWorkerGroup"), NioIoHandler.newFactory());

    private final InetSocketAddress address;

    public HttpServer(InetSocketAddress address) {
        this.address = address;
    }

    public void start() {
        try {
            new ServerBootstrap()
                    .channelFactory(NioServerSocketChannel::new)
                    .group(bossGroup, workerGroup)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(65536));

                            p.addLast(new RouterInboundHandler(router));
                        }
                    })
                    .bind(address)
                    .addListener(_ -> Thread.currentThread().setName("HttpServer"))
                    .sync();

            CloudLogger.get().info("§bHTTP server §rhas been §aestablished §ron §b{}§r.", address.toString());
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to establish HTTP server", e);
        }
    }

    public void close() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
    }
 }