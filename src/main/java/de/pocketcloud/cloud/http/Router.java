package de.pocketcloud.cloud.http;

import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.handler.RouteHandler;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.http.util.RouteDefinition;
import de.pocketcloud.cloud.http.util.RouteHandlerMethod;
import io.netty.handler.codec.http.HttpMethod;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Accessors(fluent = true)
public final class Router {

    @Getter
    private static Router instance = null;

    public static HttpMethod QUERY = new HttpMethod("QUERY");

    private final List<RouteDefinition> routes = new ArrayList<>();

    public Router() {
        instance = this;
    }

    public void registerController(Object controller) {
        routes.addAll(scanController(controller));
    }

    public void get(String path, RouteHandler handler) {
        addRoute(HttpMethod.GET, path, handler);
    }

    public void post(String path, RouteHandler handler) {
        addRoute(HttpMethod.POST, path, handler);
    }

    public void put(String path, RouteHandler handler) {
        addRoute(HttpMethod.PUT, path, handler);
    }

    public void patch(String path, RouteHandler handler) {
        addRoute(HttpMethod.PATCH, path, handler);
    }

    public void delete(String path, RouteHandler handler) {
        addRoute(HttpMethod.DELETE, path, handler);
    }

    public void options(String path, RouteHandler handler) {
        addRoute(HttpMethod.OPTIONS, path, handler);
    }

    public void head(String path, RouteHandler handler) {
        addRoute(HttpMethod.HEAD, path, handler);
    }

    public void trace(String path, RouteHandler handler) {
        addRoute(HttpMethod.TRACE, path, handler);
    }

    public void connect(String path, RouteHandler handler) {
        addRoute(HttpMethod.CONNECT, path, handler);
    }

    public void query(String path, RouteHandler handler) {
        addRoute(QUERY, path, handler);
    }

    private void addRoute(HttpMethod method, String path, RouteHandler handler) {
        Pattern pattern = Pattern.compile(RouteDefinition.toRegex(path));
        RouteHandlerMethod rhm = new RouteHandlerMethod(handler, RouteHandler.class.getMethods()[0]) {

            @Override
            public void handle(HttpRequest req, HttpResponse res) {
                handler.handle(req, res);
            }
        };

        routes.add(new RouteDefinition(method.name(), path, pattern, rhm));
    }

    public void handle(HttpRequest req, HttpResponse res) {
        String path = req.path();
        String method = req.method().name();

        for (RouteDefinition route : routes) {
            if (!route.method().equals(method)) continue;

            Matcher matcher = route.pattern().matcher(path);
            if (matcher.matches()) {
                List<String> groupNames = extractGroupNames(route.path());
                for (String name : groupNames) {
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

            if (method.isAnnotationPresent(GetRoute.class))
                result.add(buildDefinition(HttpMethod.GET, method.getAnnotation(GetRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(PostRoute.class))
                result.add(buildDefinition(HttpMethod.POST, method.getAnnotation(PostRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(PutRoute.class))
                result.add(buildDefinition(HttpMethod.PUT, method.getAnnotation(PutRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(PatchRoute.class))
                result.add(buildDefinition(HttpMethod.PATCH, method.getAnnotation(PatchRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(DeleteRoute.class))
                result.add(buildDefinition(HttpMethod.DELETE, method.getAnnotation(DeleteRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(HeadRoute.class))
                result.add(buildDefinition(HttpMethod.HEAD, method.getAnnotation(HeadRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(OptionsRoute.class))
                result.add(buildDefinition(HttpMethod.OPTIONS, method.getAnnotation(OptionsRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(TraceRoute.class))
                result.add(buildDefinition(HttpMethod.TRACE, method.getAnnotation(TraceRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(ConnectRoute.class))
                result.add(buildDefinition(HttpMethod.CONNECT, method.getAnnotation(ConnectRoute.class).value(), controller, method));
            if (method.isAnnotationPresent(QueryRoute.class))
                result.add(buildDefinition(QUERY, method.getAnnotation(QueryRoute.class).value(), controller, method));
        }

        return result;
    }

    private RouteDefinition buildDefinition(HttpMethod httpMethod, String path, Object instance, Method method) {
        Pattern pattern = Pattern.compile(RouteDefinition.toRegex(path));
        return new RouteDefinition(httpMethod.name(), path, pattern, new RouteHandlerMethod(instance, method));
    }

    /** Extracts {name} segments from a path template. */
    private List<String> extractGroupNames(String path) {
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("\\{([^/]+)}").matcher(path);
        while (m.find()) names.add(m.group(1));
        return names;
    }
}
