package de.pocketcloud.cloud.http.handler;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.http.Router;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;

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
        } catch (HttpException e) {
            if (!response.isSent()) {
                response.error(HttpResponseStatus.valueOf(e.statusCode()), e.getMessage());
            }
            CloudLogger.get().debug("Request rejected (" + e.statusCode() + ") for " + request.path() + ": " + e.getMessage());
        } catch (Exception e) {
            boolean debugEnabled = CloudLogger.get().isDebugMode();
            CloudLogger.get().error("Unhandled exception while routing request §b{}§r.{}" + request.path(), debugEnabled ? "" : " §8(§renable §edebug §rto view full stack trace§8)");
            if (debugEnabled) CloudLogger.get().exception(e);
            if (!response.isSent()) {
                response.internalServerError("An unexpected error occurred");
            }
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