package de.pocketcloud.cloud.cluster.net;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.security.ClusterCertificates;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.network.codec.CloudPacketDecoder;
import de.pocketcloud.network.codec.CloudPacketEncoder;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.network.request.RequestManager;
import de.pocketcloud.network.traffic.PacketTrafficListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

/**
 * TLS server and client for node-to-node connections. Uses the packet codec of cloud-network
 * (without its own encryption, TLS covers that) and mutual TLS with the cluster CA.
 */
public final class ClusterNetwork {

    public static final AttributeKey<String> CERT_NAME = AttributeKey.valueOf("pc-cluster-cert-name");
    public static final AttributeKey<Boolean> OUTBOUND = AttributeKey.valueOf("pc-cluster-outbound");
    public static final AttributeKey<String> PEER_NAME = AttributeKey.valueOf("pc-cluster-peer");
    public static final AttributeKey<String> ENDPOINT = AttributeKey.valueOf("pc-cluster-endpoint");

    private static final int PACKET_SIZE_LIMIT = 1_048_576;

    private final ClusterManager manager;
    private final ClusterPacketRegistry registry = new ClusterPacketRegistry();
    private final RequestManager requests = new RequestManager();
    private final SslContext serverContext;
    private final SslContext clientContext;
    private final String codecToken;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private Channel serverChannel;

    private final PacketTrafficListener listener = new PacketTrafficListener() {
        @Override
        public Packet onPacketResolve(String packetName) {
            return registry.create(packetName);
        }

        @Override
        public boolean onOutgoing(Channel channel, Packet packet, byte[] payload, int length) {
            if (packet instanceof RequestPacket request) requests.add(request);
            return true;
        }

        @Override
        public boolean onIncoming(Channel channel, byte[] payload, int length) {
            return true;
        }

        @Override
        public void onUnknownPacket(Channel channel, byte[] payload, int length) {
            CloudLogger.get().debug("Unknown cluster packet ({} bytes) from {}", length, channel.remoteAddress());
        }
    };

    public ClusterNetwork(ClusterManager manager, ClusterCertificates certificates) throws Exception {
        this.manager = manager;
        this.serverContext = certificates.serverContext();
        this.clientContext = certificates.clientContext();
        // All nodes of one cluster share this value; packets of another cluster fail the codec check.
        this.codecToken = ClusterCertificates.serialOf(certificates.caCertificate());
        IoHandlerFactory factory = Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory();
        this.bossGroup = new MultiThreadIoEventLoopGroup(1, factory);
        this.workerGroup = new MultiThreadIoEventLoopGroup(2, factory);
    }

    public void start(String bindAddress, int port) throws InterruptedException {
        serverChannel = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channelFactory(Epoll.isAvailable() ? EpollServerSocketChannel::new : NioServerSocketChannel::new)
                .childHandler(initializer(false, null, null))
                .bind(bindAddress, port)
                .sync()
                .channel();
    }

    /**
     * Opens a connection; the hello is sent once TLS is established.
     */
    public ChannelFuture connect(String host, int port, String endpoint) {
        return new Bootstrap()
                .group(workerGroup)
                .channelFactory(Epoll.isAvailable() ? EpollSocketChannel::new : NioSocketChannel::new)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .handler(initializer(true, host, port))
                .attr(ENDPOINT, endpoint)
                .connect(host, port);
    }

    public void send(Channel channel, CloudPacket packet) {
        if (channel != null && channel.isActive()) channel.writeAndFlush(packet);
    }

    public ClusterPacketRegistry registry() {
        return registry;
    }

    public RequestManager requests() {
        return requests;
    }

    public void close() {
        try {
            if (serverChannel != null) serverChannel.close().syncUninterruptibly();
        } finally {
            bossGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS);
            workerGroup.shutdownGracefully(0, 2, TimeUnit.SECONDS);
        }
    }

    private ChannelInitializer<Channel> initializer(boolean outbound, String host, Integer port) {
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) {
                ch.attr(OUTBOUND).set(outbound);
                SslHandler ssl = outbound
                        ? clientContext.newHandler(ch.alloc(), host, port)
                        : serverContext.newHandler(ch.alloc());
                ch.pipeline().addLast(
                        ssl,
                        new ReadTimeoutHandler(30),
                        new CloudPacketDecoder(() -> false, () -> PACKET_SIZE_LIMIT, () -> codecToken, listener),
                        new CloudPacketEncoder(() -> false, () -> PACKET_SIZE_LIMIT, () -> codecToken, listener),
                        new Handler()
                );
            }
        };
    }

    private final class Handler extends SimpleChannelInboundHandler<CloudPacket> {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof SslHandshakeCompletionEvent event) {
                if (!event.isSuccess()) {
                    String host = ctx.channel().remoteAddress() instanceof InetSocketAddress a ? a.getHostString() : String.valueOf(ctx.channel().remoteAddress());
                    if (manager.throttle("tls:" + host)) {
                        CloudLogger.get().warn("TLS handshake with node connection from §b{} §cfailed§r: {}", host, event.cause().getMessage());
                    }
                    ctx.close();
                    return;
                }

                String name = peerCertificateName(ctx);
                if (name == null) {
                    ctx.close();
                    return;
                }

                ctx.channel().attr(CERT_NAME).set(name);
                if (Boolean.TRUE.equals(ctx.channel().attr(OUTBOUND).get())) manager.onConnected(ctx.channel());
            }
            super.userEventTriggered(ctx, evt);
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, CloudPacket packet) {
            if (ctx.channel().attr(CERT_NAME).get() == null) {
                ctx.close();
                return;
            }

            if (packet instanceof ResponsePacket response) requests.resolve(response);
            try {
                manager.onPacket(ctx.channel(), packet);
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to handle cluster packet {}", e, packet.getName());
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            manager.onDisconnected(ctx.channel());
            super.channelInactive(ctx);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            if (!(cause instanceof io.netty.handler.codec.DecoderException && cause.getCause() instanceof javax.net.ssl.SSLException)) {
                CloudLogger.get().debug("Cluster connection {} closed: {}", ctx.channel().remoteAddress(), cause.getMessage());
            }
            ctx.close();
        }

        private String peerCertificateName(ChannelHandlerContext ctx) {
            try {
                SslHandler ssl = ctx.pipeline().get(SslHandler.class);
                Certificate[] chain = ssl.engine().getSession().getPeerCertificates();
                if (chain.length == 0 || !(chain[0] instanceof X509Certificate cert)) return null;
                return ClusterCertificates.commonName(cert);
            } catch (SSLPeerUnverifiedException e) {
                return null;
            }
        }
    }

    public static String remote(Channel channel) {
        return channel.remoteAddress() instanceof InetSocketAddress address
                ? address.getHostString() + ":" + address.getPort()
                : String.valueOf(channel.remoteAddress());
    }
}
