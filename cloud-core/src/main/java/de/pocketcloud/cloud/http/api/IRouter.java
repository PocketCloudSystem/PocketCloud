package de.pocketcloud.cloud.http.api;

import de.pocketcloud.cloud.http.auth.DefaultAuthFailedHandler;
import de.pocketcloud.cloud.http.auth.DefaultAuthentication;
import de.pocketcloud.cloud.http.auth.IAuthentication;
import de.pocketcloud.cloud.http.handler.AuthenticationFailedHandler;
import de.pocketcloud.cloud.http.handler.RouteHandler;

public interface IRouter {

    default void get(String path, RouteHandler handler) {
        get(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void get(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        get(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void get(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);

    default void post(String path, RouteHandler handler) {
        post(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void post(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        post(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void post(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);

    default void put(String path, RouteHandler handler) {
        put(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void put(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        put(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void put(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);

    default void patch(String path, RouteHandler handler) {
        patch(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void patch(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        patch(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void patch(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);

    default void delete(String path, RouteHandler handler) {
        delete(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void delete(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        delete(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void delete(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);

    default void query(String path, RouteHandler handler) {
        query(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void query(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        query(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void query(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);

    default void head(String path, RouteHandler handler) {
        head(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void head(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        head(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void head(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);

    default void options(String path, RouteHandler handler) {
        options(path, handler, DefaultAuthentication.class, DefaultAuthFailedHandler.class);
    }

    default void options(String path, RouteHandler handler, Class<? extends IAuthentication> auth) {
        options(path, handler, auth, DefaultAuthFailedHandler.class);
    }

    void options(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed);
}