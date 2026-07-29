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

    public static final int UNVERSIONED = -1;

    private final Map<Class<?>, Object> instanceCache = new HashMap<>();
    private final Map<Integer, VersionMeta> versionMeta = new HashMap<>();
    @Getter
    private final List<RouteDefinition> routes = new ArrayList<>();

    public Router() {
        registerController(new TestRoutes());
    }

    public void registerController(Object controller) {
        routes.addAll(scanController(controller));
    }

    public void deprecateVersion(int version, String sunset) {
        versionMeta.put(version, new VersionMeta(true, sunset));
    }

    public void get(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.GET, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void get(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(HttpMethod.GET, path, handler, auth, onAuthFailed, version);
    }

    public void post(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.POST, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void post(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(HttpMethod.POST, path, handler, auth, onAuthFailed, version);
    }

    public void put(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.PUT, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void put(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(HttpMethod.PUT, path, handler, auth, onAuthFailed, version);
    }

    public void patch(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.PATCH, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void patch(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(HttpMethod.PATCH, path, handler, auth, onAuthFailed, version);
    }

    public void delete(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.DELETE, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void delete(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(HttpMethod.DELETE, path, handler, auth, onAuthFailed, version);
    }

    public void query(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(QUERY, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void query(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(QUERY, path, handler, auth, onAuthFailed, version);
    }

    public void head(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.HEAD, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void head(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(HttpMethod.HEAD, path, handler, auth, onAuthFailed, version);
    }

    public void options(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed) {
        addRoute(HttpMethod.OPTIONS, path, handler, auth, onAuthFailed, UNVERSIONED);
    }

    public void options(String path, RouteHandler handler, Class<? extends IAuthentication> auth, Class<? extends AuthenticationFailedHandler> onAuthFailed, int version) {
        addRoute(HttpMethod.OPTIONS, path, handler, auth, onAuthFailed, version);
    }

    private void addRoute(HttpMethod method, String path, RouteHandler handler, Class<? extends IAuthentication> authClass, Class<? extends AuthenticationFailedHandler> failedAuthClass, int version) {
        try {
            RouteHandlerMethod rhm = wrapWithAuth(handler, authClass, failedAuthClass);
            String finalPath = version == UNVERSIONED ? path : versionedPath(version, path);
            Pattern pattern = Pattern.compile(RouteDefinition.toRegex(finalPath));
            routes.add(new RouteDefinition(method.name(), finalPath, pattern, rhm, version));
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

                applyVersionHeaders(res, route.version());
                route.handler().handle(req, res);
                return;
            }
        }

        res.status(404).text("404 Not Found");
    }

    private void applyVersionHeaders(HttpResponse res, int version) {
        if (version == UNVERSIONED) return;
        res.header("X-API-Version", String.valueOf(version));

        VersionMeta meta = versionMeta.get(version);
        if (meta != null && meta.deprecated()) {
            res.header("Deprecation", "true");
            if (meta.sunset() != null && !meta.sunset().isBlank()) {
                res.header("Sunset", meta.sunset());
            }
        }
    }

    private int resolveVersion(Class<?> controllerClass, int methodVersion) {
        if (methodVersion != UNVERSIONED) return methodVersion;

        ApiVersion classVersion = controllerClass.getAnnotation(ApiVersion.class);
        return classVersion != null ? classVersion.value() : UNVERSIONED;
    }

    private String versionedPath(int version, String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        return "/v" + version + normalized;
    }

    private List<RouteDefinition> scanController(Object controller) {
        List<RouteDefinition> result = new ArrayList<>();
        Class<?> clazz = controller.getClass();

        ApiVersion classVersion = clazz.getAnnotation(ApiVersion.class);
        if (classVersion != null && classVersion.deprecated()) {
            versionMeta.put(classVersion.value(), new VersionMeta(true, classVersion.sunset()));
        }

        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);

            registerIfPresent(result, method, controller, GetRoute.class, HttpMethod.GET, GetRoute::value, GetRoute::authentication, GetRoute::onAuthFailed, GetRoute::version);
            registerIfPresent(result, method, controller, PostRoute.class, HttpMethod.POST, PostRoute::value, PostRoute::authentication, PostRoute::onAuthFailed, PostRoute::version);
            registerIfPresent(result, method, controller, PutRoute.class, HttpMethod.PUT, PutRoute::value, PutRoute::authentication, PutRoute::onAuthFailed, PutRoute::version);
            registerIfPresent(result, method, controller, PatchRoute.class, HttpMethod.PATCH, PatchRoute::value, PatchRoute::authentication, PatchRoute::onAuthFailed, PatchRoute::version);
            registerIfPresent(result, method, controller, DeleteRoute.class, HttpMethod.DELETE, DeleteRoute::value, DeleteRoute::authentication, DeleteRoute::onAuthFailed, DeleteRoute::version);
            registerIfPresent(result, method, controller, HeadRoute.class, HttpMethod.HEAD, HeadRoute::value, HeadRoute::authentication, HeadRoute::onAuthFailed, HeadRoute::version);
            registerIfPresent(result, method, controller, OptionsRoute.class, HttpMethod.OPTIONS, OptionsRoute::value, OptionsRoute::authentication, OptionsRoute::onAuthFailed, OptionsRoute::version);
            registerIfPresent(result, method, controller, QueryRoute.class, QUERY, QueryRoute::value, QueryRoute::authentication, QueryRoute::onAuthFailed, QueryRoute::version);
        }

        return result;
    }

    private <A extends java.lang.annotation.Annotation> void registerIfPresent(
            List<RouteDefinition> result, Method method, Object controller,
            Class<A> annotationType, HttpMethod httpMethod,
            Function<A, String> pathExtractor,
            Function<A, Class<? extends IAuthentication>> authExtractor,
            Function<A, Class<? extends AuthenticationFailedHandler>> failExtractor,
            Function<A, Integer> versionExtractor) {

        A annotation = method.getAnnotation(annotationType);
        if (annotation == null) return;

        int version = resolveVersion(controller.getClass(), versionExtractor.apply(annotation));
        String rawPath = pathExtractor.apply(annotation);
        String path = version == UNVERSIONED ? rawPath : versionedPath(version, rawPath);

        RouteHandler handler = new RouteHandlerMethod(controller, method);

        try {
            RouteHandlerMethod rhm = wrapWithAuth(handler, authExtractor.apply(annotation), failExtractor.apply(annotation));
            Pattern pattern = Pattern.compile(RouteDefinition.toRegex(path));
            result.add(new RouteDefinition(httpMethod.name(), path, pattern, rhm, version));
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

    private record VersionMeta(boolean deprecated, String sunset) {}
}