package de.pocketcloud.cloud.http.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the default API version for all routes of a controller.
 * <p>
 * A single route method can still override this via the {@code version()}
 * attribute on its own {@code @*Route} annotation (e.g. {@link GetRoute#version()}).
 * <p>
 * If {@code deprecated} is set, the {@link de.pocketcloud.cloud.http.Router} will
 * automatically attach {@code Deprecation} / {@code Sunset} response headers
 * (see RFC 8594) to every request served by a route of this version.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiVersion {

    int value();

    boolean deprecated() default false;

    /**
     * ISO-8601 date/time (e.g. "2026-12-31") at which this version will be
     * removed. Only relevant if {@link #deprecated()} is {@code true}.
     */
    String sunset() default "";
}
