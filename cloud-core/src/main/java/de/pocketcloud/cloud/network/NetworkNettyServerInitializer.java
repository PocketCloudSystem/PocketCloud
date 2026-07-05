package de.pocketcloud.cloud.network;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.MainConfig;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.network.codec.CloudPacketDecoder;
import de.pocketcloud.network.codec.CloudPacketEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class NetworkNettyServerInitializer extends ChannelInitializer<Channel> {

    @Override
    protected void initChannel(Channel channel) {
        channel.pipeline().addLast(
            new CloudPacketDecoder(() -> MainConfig.instance().isNetworkEncryptionEnabled(), () -> PocketCloud.instance().network().authToken(), (addr, buf, _) -> {
                CloudLogger.get().debug("Received unknown packet from {}", addr.toString())
                        .debug(buf);
            }),
            new CloudPacketEncoder(() -> MainConfig.instance().isNetworkEncryptionEnabled(), () -> PocketCloud.instance().network().authToken()),
            new NetworkNettyHandler()
        );
    }
}