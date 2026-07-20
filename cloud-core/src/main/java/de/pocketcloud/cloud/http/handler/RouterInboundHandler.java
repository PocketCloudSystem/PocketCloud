package de.pocketcloud.cloud.http.handler;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.http.Router;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

import java.io.IOException;

public final class RouterInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Router router;

    public RouterInboundHandler(Router router) {
        this.router = router;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        if (!req.decoderResult().isSuccess()) {
            new HttpResponse(ctx).badRequest("Malformed HTTP request");
            return;
        }

        HttpRequest request = new HttpRequest(req, ctx.channel());
        HttpResponse response = new HttpResponse(ctx).keepAlive(request.isKeepAlive());

        try {
            router.handle(request, response);
        } catch (Exception e) {
            response.internalServerError("An unexpected error occurred: " + e.getMessage());
            CloudLogger.get().exception("Unhandled exception while routing request " + request.path(), e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof IOException) {
            ctx.close();
            return;
        }

        CloudLogger.get().exception("Unexpected exception in HTTP pipeline", cause);
        ctx.close();
    }
}