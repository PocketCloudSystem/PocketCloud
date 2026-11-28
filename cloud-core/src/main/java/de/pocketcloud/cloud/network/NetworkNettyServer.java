package de.pocketcloud.cloud.network;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.util.StringUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import java.util.concurrent.TimeUnit;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

@Getter
@Accessors(fluent = true)
public class NetworkNettyServer {

    private final SocketAddress address;
    private final boolean encryption;
    private final int packetSizeLimit;
    @Getter(AccessLevel.NONE)
    private final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final int AUTH_TOKEN_LENGTH = 32;

    /**
     * Persisted across restarts (storage/internal/auth-token), so servers can reconnect
     * after the cloud has been restarted.
     */
    private final String authToken = loadOrCreateAuthToken(PocketCloudPaths.storage().internal().with("auth-token").asPath());

    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory("boss-group"),
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory()
    );

    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(
            new DefaultThreadFactory("worker-group"),
            Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory()
    );

    public NetworkNettyServer(SocketAddress address, boolean encryption, int packetSizeLimit) {
        this.address = address;
        this.encryption = encryption;
        this.packetSizeLimit = packetSizeLimit;
    }

    static String loadOrCreateAuthToken(Path tokenPath) {
        try {
            if (Files.isRegularFile(tokenPath)) {
                String stored = Files.readString(tokenPath).trim();
                if (stored.length() >= AUTH_TOKEN_LENGTH && stored.chars().noneMatch(Character::isWhitespace)) {
                    return stored;
                }

                CloudLogger.get().warn("Stored network auth token is invalid, generating a new one...");
            }

            String token = StringUtils.generate(AUTH_TOKEN_LENGTH);
            if (tokenPath.getParent() != null) Files.createDirectories(tokenPath.getParent());
            Files.writeString(tokenPath, token);

            try {
                Files.setPosixFilePermissions(tokenPath, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException | IOException _) {
                // non-posix filesystem, ignore
            }

            return token;
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to persist the network auth token, using a temporary one", e);
            return StringUtils.generate(AUTH_TOKEN_LENGTH);
        }
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
        // no quiet period: the default (2s) delayed every exit of the cloud
        this.bossGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
        this.workerGroup.shutdownGracefully(0, 1, TimeUnit.SECONDS);
    }

    public void addChannel(Channel channel) {
        if (channels.add(channel)) {
            CloudLogger.get().debug("Client connected: {}", channel.remoteAddress());
        }
    }

    public void removeChannel(Channel channel) {
        if (channels.remove(channel)) {
            CloudLogger.get().debug("Client disconnected: {}", channel.remoteAddress());
        }
    }

    public boolean isChannelActive(Channel channel) {
        return channels.find(channel.id()) != null;
    }
}