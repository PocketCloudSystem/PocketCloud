import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollIoHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.SocketAddress;

public class NettyClient {

    private static final EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(Epoll.isAvailable() ? EpollIoHandler.newFactory() : NioIoHandler.newFactory());

    public void connect(SocketAddress address) {
        new Bootstrap()
                .group(eventLoopGroup)
                .channelFactory(Epoll.isAvailable() ? EpollSocketChannel::new : NioSocketChannel::new)
                .handler(new NettyClientInitializer())
                .connect(address).addListener(future -> {
                    if (future.isSuccess()) {
                        System.out.println("Connected to " + address);
                    } else {
                        System.out.println("Failed to connect to " + address);
                        future.cause().printStackTrace();
                    }
                });
    }
}
