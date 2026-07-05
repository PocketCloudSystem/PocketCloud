package de.pocketcloud.cloud.http.io;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.CharsetUtil;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private final Map<String, String> pathParams = new HashMap<>();
    private final FullHttpRequest request;

    public HttpRequest(FullHttpRequest request) {
        this.request = request;
    }

    public void setPathParam(String key, String value) {
        pathParams.put(key, value);
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

    public String pathParam(String key) {
        return pathParams.get(key);
    }

    public static String toRegex(String path) {
        return path.replaceAll("\\{([^/]+)}", "(?<$1>[^/]+)");
    }
}
