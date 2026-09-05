package de.pocketcloud.cloud.http.util;

import com.google.gson.JsonSyntaxException;
import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.annotation.RequestBody;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.handler.RouteHandler;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.common.util.FileUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class RouteHandlerMethod implements RouteHandler {

    private final Object instance;
    private final Method method;

    public RouteHandlerMethod(Object instance, Method method) {
        this.instance = instance;
        this.method = method;
    }

    @Override
    public void handle(HttpRequest req, HttpResponse res) {
        Object[] args;
        try {
            args = resolveArguments(req, res);
        } catch (JsonSyntaxException e) {
            throw new HttpException(400, "Invalid JSON body: " + e.getMessage());
        } catch (Exception e) {
            throw new HttpException(400, "Invalid request parameters");
        }

        try {
            method.invoke(instance, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof HttpException httpEx) throw httpEx;
            throw new RuntimeException("Route handler threw an exception", cause);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new RuntimeException("Failed to invoke route handler (check parameter types)", e);
        }
    }

    private Object[] resolveArguments(HttpRequest req, HttpResponse res) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            if (param.isAnnotationPresent(PathVariable.class)) {
                String name = param.getAnnotation(PathVariable.class).value();
                args[i] = req.pathParam(name);
            } else if (param.isAnnotationPresent(RequestBody.class)) {
                Class<?> type = param.getType();
                args[i] = type == String.class ? req.body() : FileUtils.GSON.fromJson(req.body(), type);
            } else if (param.getType() == HttpRequest.class) {
                args[i] = req;
            } else if (param.getType() == HttpResponse.class) {
                args[i] = res;
            } else {
                args[i] = null;
            }
        }

        return args;
    }
}