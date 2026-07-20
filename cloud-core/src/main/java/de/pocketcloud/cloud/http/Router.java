package de.pocketcloud.cloud.http;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.api.IRouter;
import de.pocketcloud.cloud.http.auth.IAuthentication;
import de.pocketcloud.cloud.http.handler.AuthenticationFailedHandler;
import de.pocketcloud.cloud.http.handler.RouteHandler;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.http.route.TestRoutes;
import de.pocketcloud.cloud.http.util.RouteDefinition;
import de.pocketcloud.cloud.http.util.RouteHandlerMethod;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Accessors(fluent = true)
public final class Router implements IRouter {

    public static final HttpMethod QUERY = new HttpMethod("QUERY");

    private final Map<Class<?>, Object> instanceCache = new HashMap<>();
    @Getter
    private final List<RouteDefinition> routes = new ArrayList<>();

    public Router() {
        registerController(new TestRoutes());
    }

    public void registerController(Object controller) {
        routes.addAll(scanController(controller));
    }

    public void get(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.GET, path, handler, auth, onAuthFailed);
    }

    public void post(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.POST, path, handler, auth, onAuthFailed);
    }

    public void put(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.PUT, path, handler, auth, onAuthFailed);
    }

    public void patch(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.PATCH, path, handler, auth, onAuthFailed);
    }

    public void delete(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.DELETE, path, handler, auth, onAuthFailed);
    }

    public void query(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(QUERY, path, handler, auth, onAuthFailed);
    }

    public void head(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.HEAD, path, handler, auth, onAuthFailed);
    }

    public void options(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.OPTIONS, path, handler, auth, onAuthFailed);
    }

    private void addRoute(HttpMethod method, String path, RouteHandler handler, Class<? extends IAuthentication> authClass, Class<? extends AuthenticationFailedHandler> failedAuthClass) {
        try {
            RouteHandlerMethod rhm = wrapWithAuth(handler, authClass, failedAuthClass);
            Pattern pattern = Pattern.compile(RouteDefinition.toRegex(path));
            routes.add(new RouteDefinition(method.name(), path, pattern, rhm));
        } catch (Exception e) {
            CloudLogger.get().error("Failed to add route: " + path, e);
        }
    }

    private RouteHandlerMethod wrapWithAuth(RouteHandler handler, Class<? extends IAuthentication> authClass, Class<? extends AuthenticationFailedHandler> failedAuthClass) {
        IAuthentication auth = instanceOf(authClass);
        AuthenticationFailedHandler onFail = instanceOf(failedAuthClass);

        return new RouteHandlerMethod(handler, RouteHandler.class.getMethods()[0]) {

            @Override
            public void handle(HttpRequest req, HttpResponse res) {
                if (!auth.authenticated(req)) {
                    onFail.handle(req, res);
                    return;
                }

                handler.handle(req, res);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T> T instanceOf(Class<? extends T> clazz) {
        return (T) instanceCache.computeIfAbsent(clazz, c -> {
            try {
                return c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate " + c, e);
            }
        });
    }

    public void handle(HttpRequest req, HttpResponse res) {
        String path = req.path();
        String method = req.method().name();

        for (RouteDefinition route : routes) {
            if (!route.method().equals(method)) continue;

            Matcher matcher = route.pattern().matcher(path);
            if (matcher.matches()) {
                for (String name : extractGroupNames(route.path())) {
                    try {
                        req.setPathParam(name, matcher.group(name));
                    } catch (IllegalArgumentException ignored) {}
                }

                route.handler().handle(req, res);
                return;
            }
        }

        res.status(404).text("404 Not Found");
    }

    private List<RouteDefinition> scanController(Object controller) {
        List<RouteDefinition> result = new ArrayList<>();
        Class<?> clazz = controller.getClass();

        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);

            registerIfPresent(result, method, controller, GetRoute.class, HttpMethod.GET, GetRoute::value, GetRoute::authentication, GetRoute::onAuthFailed);
            registerIfPresent(result, method, controller, PostRoute.class, HttpMethod.POST, PostRoute::value, PostRoute::authentication, PostRoute::onAuthFailed);
            registerIfPresent(result, method, controller, PutRoute.class, HttpMethod.PUT, PutRoute::value, PutRoute::authentication, PutRoute::onAuthFailed);
            registerIfPresent(result, method, controller, PatchRoute.class, HttpMethod.PATCH, PatchRoute::value, PatchRoute::authentication, PatchRoute::onAuthFailed);
            registerIfPresent(result, method, controller, DeleteRoute.class, HttpMethod.DELETE, DeleteRoute::value, DeleteRoute::authentication, DeleteRoute::onAuthFailed);
            registerIfPresent(result, method, controller, HeadRoute.class, HttpMethod.HEAD, HeadRoute::value, HeadRoute::authentication, HeadRoute::onAuthFailed);
            registerIfPresent(result, method, controller, OptionsRoute.class, HttpMethod.OPTIONS, OptionsRoute::value, OptionsRoute::authentication, OptionsRoute::onAuthFailed);
            registerIfPresent(result, method, controller, QueryRoute.class, QUERY, QueryRoute::value, QueryRoute::authentication, QueryRoute::onAuthFailed);
        }

        return result;
    }

    private <A extends java.lang.annotation.Annotation> void registerIfPresent(
            List<RouteDefinition> result, Method method, Object controller,
            Class<A> annotationType, HttpMethod httpMethod,
            Function<A, String> pathExtractor,
            Function<A, Class<? extends IAuthentication>> authExtractor,
            Function<A, Class<? extends AuthenticationFailedHandler>> failExtractor) {

        A annotation = method.getAnnotation(annotationType);
        if (annotation == null) return;

        String path = pathExtractor.apply(annotation);
        RouteHandler handler = new RouteHandlerMethod(controller, method); // <-- statt der naiven Lambda

        try {
            RouteHandlerMethod rhm = wrapWithAuth(handler, authExtractor.apply(annotation), failExtractor.apply(annotation));
            Pattern pattern = Pattern.compile(RouteDefinition.toRegex(path));
            result.add(new RouteDefinition(httpMethod.name(), path, pattern, rhm));
        } catch (Exception e) {
            CloudLogger.get().error("Failed to register annotated route: " + path, e);
        }
    }

    private List<String> extractGroupNames(String path) {
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("\\{([^/]+)}").matcher(path);
        while (m.find()) names.add(m.group(1));
        return names;
    }
}