package de.pocketcloud.cloud.http.handler;

import de.pocketcloud.cloud.http.Router;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

public class RouterInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Router router;

    public RouterInboundHandler(Router router) {
        this.router = router;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        HttpRequest request = new HttpRequest(req);
        HttpResponse response = new HttpResponse(ctx);

        router.handle(request, response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!cause.getMessage().contains("Connection reset")) {
            super.exceptionCaught(ctx, cause);
        }
    }
}