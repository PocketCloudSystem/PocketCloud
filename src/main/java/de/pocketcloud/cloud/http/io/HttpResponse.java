package de.pocketcloud.cloud.http.io;

import com.google.gson.JsonObject;
import de.pocketcloud.cloud.util.FileUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.function.Consumer;

public final class HttpResponse {

    private final ChannelHandlerContext ctx;
    private HttpResponseStatus status = HttpResponseStatus.OK;

    public HttpResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public HttpResponse status(int code) {
        this.status = HttpResponseStatus.valueOf(code);
        return this;
    }

    public void text(String message) {
        send("text/plain", message);
    }

    public void json(String json) {
        send("application/json", json);
    }

    public void json(Consumer<JsonObject> json) {
        JsonObject obj = new JsonObject();
        json.accept(obj);
        json(FileUtils.encodeJson(obj));
    }

    private void send(String contentType, String body) {
        ByteBuf content = Unpooled.copiedBuffer(body, CharsetUtil.UTF_8);

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());

        ctx.writeAndFlush(response);
    }
}
