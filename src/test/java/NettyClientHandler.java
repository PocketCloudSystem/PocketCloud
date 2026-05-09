import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import packet.CloudPacket;
import packet.impl.TestPacket;

public final class NettyClientHandler extends SimpleChannelInboundHandler<CloudPacket> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(new TestPacket("hi wie gehts dir bro")).addListener(future -> {
            System.out.println(future.isSuccess());
            System.out.println(future.isDone());
            if (!future.isSuccess()) future.cause().printStackTrace();
        });
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, CloudPacket cloudPacket) throws Exception {
        System.out.println("Received packet");
        System.out.println(cloudPacket.getName());
    }
}
