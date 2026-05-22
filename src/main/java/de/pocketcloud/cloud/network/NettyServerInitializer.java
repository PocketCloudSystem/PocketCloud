package de.pocketcloud.cloud.network;

import de.pocketcloud.cloud.network.packet.codec.CloudPacketDecoder;
import de.pocketcloud.cloud.network.packet.codec.CloudPacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class NettyServerInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(
            new CloudPacketDecoder(),
            new CloudPacketEncoder(),
            new NettyHandler()
        );
    }
}