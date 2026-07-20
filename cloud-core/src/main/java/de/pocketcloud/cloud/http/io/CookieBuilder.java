package de.pocketcloud.cloud.http.io;

import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.time.Duration;

public final class CookieBuilder {

    public enum SameSite {
        STRICT, LAX, NONE
    }

    private final HttpResponse response;
    private final String name;
    private final String value;

    private String path = "/";
    private String domain;
    private boolean httpOnly = true;
    private boolean secure = false;
    private long maxAge = Long.MIN_VALUE;
    private SameSite sameSite;

    public CookieBuilder(HttpResponse response, String name, String value) {
        this.response = response;
        this.name = name;
        this.value = value;
    }

    public CookieBuilder path(String path) {
        this.path = path;
        return this;
    }

    public CookieBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    public CookieBuilder httpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
        return this;
    }

    public CookieBuilder secure(boolean secure) {
        this.secure = secure;
        return this;
    }

    public CookieBuilder maxAge(long seconds) {
        this.maxAge = seconds;
        return this;
    }

    public CookieBuilder expiresIn(Duration duration) {
        this.maxAge = duration.getSeconds();
        return this;
    }

    public CookieBuilder sameSite(SameSite sameSite) {
        this.sameSite = sameSite;
        return this;
    }

    public CookieBuilder session() {
        this.maxAge = Long.MIN_VALUE;
        return this;
    }

    void add() {
        DefaultCookie cookie = new DefaultCookie(name, value);
        cookie.setPath(path);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);

        if (domain != null) {
            cookie.setDomain(domain);
        }
        if (maxAge != Long.MIN_VALUE) {
            cookie.setMaxAge(maxAge);
        }

        String encoded = ServerCookieEncoder.STRICT.encode(cookie);

        if (sameSite != null) {
            encoded += "; SameSite=" + formatSameSite(sameSite);
        }

        response.addCookieHeader(encoded);
    }

    private String formatSameSite(SameSite sameSite) {
        return switch (sameSite) {
            case STRICT -> "Strict";
            case LAX -> "Lax";
            case NONE -> "None";
        };
    }
}