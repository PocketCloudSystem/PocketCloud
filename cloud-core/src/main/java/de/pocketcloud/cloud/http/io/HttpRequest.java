package de.pocketcloud.cloud.http.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pocketcloud.common.util.FileUtils;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;

public final class HttpRequest {

    private final Map<String, String> pathParams = new HashMap<>();
    private final FullHttpRequest request;
    private final Channel channel;

    private QueryStringDecoder queryDecoder;

    public HttpRequest(FullHttpRequest request, Channel channel) {
        this.request = request;
        this.channel = channel;
    }

    public void setPathParam(String key, String value) {
        pathParams.put(key, value);
    }

    public String uri() {
        return request.uri();
    }

    public HttpVersion protocolVersion() {
        return request.protocolVersion();
    }

    public String body() {
        return request.content().toString(CharsetUtil.UTF_8);
    }

    public String path() {
        String uri = request.uri();
        int queryIndex = uri.indexOf('?');
        return queryIndex >= 0 ? uri.substring(0, queryIndex) : uri;
    }

    public HttpMethod method() {
        return request.method();
    }

    public HttpHeaders headers() {
        return request.headers();
    }

    public HttpHeaders trailingHeaders() {
        return request.trailingHeaders();
    }

    public String pathParam(String key) {
        return pathParams.get(key);
    }

    private QueryStringDecoder queryDecoder() {
        if (queryDecoder == null) queryDecoder = new QueryStringDecoder(request.uri());
        return queryDecoder;
    }

    public String queryParam(String key) {
        List<String> values = queryDecoder().parameters().get(key);
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    public String queryParam(String key, String defaultValue) {
        String value = queryParam(key);
        return value != null ? value : defaultValue;
    }

    public List<String> queryParams(String key) {
        return queryDecoder().parameters().getOrDefault(key, Collections.emptyList());
    }

    public Map<String, List<String>> queryParams() {
        return queryDecoder().parameters();
    }

    public boolean hasQueryParam(String key) {
        return queryDecoder().parameters().containsKey(key);
    }

    public JsonObject json() {
        JsonElement element = JsonParser.parseString(body());
        if (!element.isJsonObject()) {
            throw new IllegalStateException("Request body is not a JSON object");
        }
        return element.getAsJsonObject();
    }

    public <T> T json(Class<T> type) {
        return FileUtils.decodeJson(body(), type);
    }

    public boolean hasBody() {
        return request.content().readableBytes() > 0;
    }

    public String header(String name) {
        return request.headers().get(name);
    }

    public String header(String name, String defaultValue) {
        return request.headers().get(name, defaultValue);
    }

    public String contentType() {
        return request.headers().get(HttpHeaderNames.CONTENT_TYPE);
    }

    public boolean isJson() {
        String contentType = contentType();
        return contentType != null && contentType.toLowerCase().contains("application/json");
    }

    public boolean isKeepAlive() {
        return HttpUtil.isKeepAlive(request);
    }

    public Optional<String> bearerToken() {
        String value = header(HttpHeaderNames.AUTHORIZATION.toString());
        if (value == null || !value.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.of(value.substring("Bearer ".length()).trim());
    }

    public String remoteAddress() {
        SocketAddress addr = channel.remoteAddress();
        if (addr instanceof InetSocketAddress inet) {
            return inet.getAddress().getHostAddress();
        }
        return addr != null ? addr.toString() : "unknown";
    }

    public static String toRegex(String path) {
        return path.replaceAll("\\{([^/]+)}", "(?<$1>[^/]+)");
    }
}