package de.pocketcloud.cloud.http.io;

import com.google.gson.JsonObject;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.traffic.HttpTrafficMonitor;
import de.pocketcloud.common.util.FileUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class HttpResponse {

    @Getter
    private boolean sent = false;

    private final ChannelHandlerContext ctx;
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final Map<String, String> headers = new HashMap<>();
    private final List<String> cookies = new ArrayList<>();
    private boolean keepAlive = false;

    public HttpResponse(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    public HttpResponse keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public HttpResponse status(int code) {
        this.status = HttpResponseStatus.valueOf(code);
        return this;
    }

    public HttpResponse status(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    public HttpResponse header(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public HttpResponse headers(Map<String, String> newHeaders) {
        headers.putAll(newHeaders);
        return this;
    }

    public HttpResponse cookie(String name, String value, Consumer<CookieBuilder> consumer) {
        CookieBuilder builder = new CookieBuilder(this, name, value);
        consumer.accept(builder);
        builder.add();
        return this;
    }

    public HttpResponse attachment(String filename) {
        headers.put(HttpHeaderNames.CONTENT_DISPOSITION.toString(), "attachment; filename=\"" + filename + "\"");
        return this;
    }

    public HttpResponse cors(String origin) {
        headers.put(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), origin);
        headers.put(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString(), "GET,POST,PUT,DELETE,OPTIONS");
        headers.put(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString(), "Content-Type,Authorization");
        return this;
    }

    void addCookieHeader(String encodedCookie) {
        cookies.add(encodedCookie);
    }

    public void text(String message) {
        send("text/plain", message);
    }

    public void html(String htmlContent) {
        send("text/html", htmlContent);
    }

    public void json(String json) {
        send("application/json", json);
    }

    public void json(JsonObject json) {
        json(FileUtils.encodeJson(json));
    }

    public void json(Consumer<JsonObject> json) {
        JsonObject obj = new JsonObject();
        json.accept(obj);
        json(obj);
    }

    public void bytes(String contentType, byte[] data) {
        ByteBuf content = Unpooled.wrappedBuffer(data);
        write(contentType, content);
    }

    public void noContent() {
        this.status = HttpResponseStatus.NO_CONTENT;
        write("text/plain", Unpooled.EMPTY_BUFFER);
    }

    public void redirect(String location) {
        this.status = HttpResponseStatus.FOUND; // 302
        headers.put(HttpHeaderNames.LOCATION.toString(), location);
        write("text/plain", Unpooled.EMPTY_BUFFER);
    }

    public void redirectPermanent(String location) {
        this.status = HttpResponseStatus.MOVED_PERMANENTLY; // 301
        headers.put(HttpHeaderNames.LOCATION.toString(), location);
        write("text/plain", Unpooled.EMPTY_BUFFER);
    }

    public void error(HttpResponseStatus status, String message) {
        this.status = status;
        JsonObject error = new JsonObject();
        error.addProperty("error", status.reasonPhrase());
        error.addProperty("message", message);
        json(error);
    }

    public void badRequest(String message) {
        error(HttpResponseStatus.BAD_REQUEST, message);
    }

    public void unauthorized(String message) {
        error(HttpResponseStatus.UNAUTHORIZED, message);
    }

    public void forbidden(String message) {
        error(HttpResponseStatus.FORBIDDEN, message);
    }

    public void notFound(String message) {
        error(HttpResponseStatus.NOT_FOUND, message);
    }

    public void internalServerError(String message) {
        error(HttpResponseStatus.INTERNAL_SERVER_ERROR, message);
    }

    public void file(File file, String contentType) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        long length = raf.length();

        DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, length);
        headers.forEach((k, v) -> response.headers().set(k, v));

        ctx.write(response);
        ctx.write(new ChunkedFile(raf, 0, length, 8192), ctx.newProgressivePromise());
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(keepAlive ? f -> {} : ChannelFutureListener.CLOSE);
    }

    private void send(String contentType, String body) {
        ByteBuf content = Unpooled.copiedBuffer(body, CharsetUtil.UTF_8);
        write(contentType, content);
    }

    private void write(String contentType, ByteBuf content) {
        if (sent) throw new IllegalStateException("Response was already sent");
        sent = true;
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, content);

        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + "; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        PocketCloud.instance().traffic().pushBytes(HttpTrafficMonitor.class, TrafficDirection.OUT, content.readableBytes());

        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        headers.forEach((key, value) -> response.headers().set(key, value));
        cookies.forEach(c -> response.headers().add(HttpHeaderNames.SET_COOKIE, c));

        if (keepAlive) {
            ctx.writeAndFlush(response);
        } else {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }
}