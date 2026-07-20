package de.pocketcloud.cloud.http;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.sub.SslConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.http.handler.RouterInboundHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.pkitesting.CertificateBuilder;
import io.netty.pkitesting.X509Bundle;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

@Getter
@Accessors(fluent = true)
public final class HttpServer {

    private final Router router = new Router();

    @Getter(AccessLevel.NONE)
    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("HttpBossGroup"), NioIoHandler.newFactory());
    @Getter(AccessLevel.NONE)
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(new DefaultThreadFactory("HttpWorkerGroup"), NioIoHandler.newFactory());

    private boolean started = false;
    private final SocketAddress address;
    private final String authToken;

    @Getter(AccessLevel.NONE)
    private SslContext sslContext = null;

    public HttpServer(SocketAddress address, String authToken) {
        this.address = address;
        this.authToken = authToken;
    }

    public void start() {
        try {
            try {
                sslContext = buildSslContext(PocketCloud.instance().config().httpServer().ssl());
            } catch (Exception e) {
                CloudLogger.get().exception("Unexpected exception caught while creating the SslContent, not starting server...", e);
                return;
            }

            if (sslContext != null) {
                CloudLogger.get().info("§bHTTP server §rwill use §aTLS§r. §8(§e{}§8).", PocketCloud.instance().config().httpServer().ssl().selfSigned() ? "self-signed" : "certificate file");
            } else {
                CloudLogger.get().info("§bHTTP server §rrunning without TLS§r.");
            }

            new ServerBootstrap()
                    .channelFactory(NioServerSocketChannel::new)
                    .group(bossGroup, workerGroup)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            if (sslContext != null) {
                                p.addLast(sslContext.newHandler(ch.alloc()));
                            }

                            p.addLast(new ChannelInboundHandlerAdapter() {

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    if (cause instanceof DecoderException de && de.getCause() instanceof NotSslRecordException) {
                                        ctx.close();
                                        return;
                                    }

                                    ctx.fireExceptionCaught(cause);
                                }
                            });

                            p.addLast(new HttpServerCodec());
                            p.addLast(new HttpObjectAggregator(10 * 1024 * 1024));
                            p.addLast(new ChunkedWriteHandler());

                            p.addLast(new RouterInboundHandler(router));
                        }
                    })
                    .bind(address)
                    .addListener(_ -> Thread.currentThread().setName("HttpServer"))
                    .sync();

            started = true;
            CloudLogger.get().info("§bHTTP server §rhas been §aestablished §ron §b{}§r.", address.toString());
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to establish HTTP server", e);
        }
    }

    public SslContext buildSslContext(SslConfiguration config) throws Exception {
        if (!config.enabled()) {
            return null;
        }

        if (config.selfSigned()) {
            X509Bundle bundle = new CertificateBuilder()
                    .subject("CN=" + config.selfSignedHostname())
                    .addSanDnsName(config.selfSignedHostname())
                    .setIsCertificateAuthority(true)
                    .buildSelfSigned();

            return SslContextBuilder
                    .forServer(bundle.toKeyManagerFactory())
                    .build();
        }

        return SslContextBuilder.forServer(
                config.certFile(),
                config.privateKeyFile(),
                config.hasKeyPassword() ? config.keyPassword() : null
        ).build();
    }

    public void close() {
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        if (sslContext != null) ReferenceCountUtil.release(sslContext);
    }
 }