import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import packet.codec.CloudPacketDecoder;
import packet.codec.CloudPacketEncoder;

public class NettyClientInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(
                new CloudPacketDecoder(),
                new CloudPacketEncoder(),
                new NettyClientHandler()
        );
    }
}
