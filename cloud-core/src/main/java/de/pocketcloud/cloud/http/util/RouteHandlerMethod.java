package de.pocketcloud.cloud.http.util;

import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.annotation.RequestBody;
import de.pocketcloud.cloud.http.handler.RouteHandler;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.common.util.FileUtils;

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
        try {
            Parameter[] parameters = method.getParameters();
            Object[] args = new Object[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter param = parameters[i];

                if (param.isAnnotationPresent(PathVariable.class)) {
                    String name = param.getAnnotation(PathVariable.class).value();
                    args[i] = req.pathParam(name);
                } else if (param.isAnnotationPresent(RequestBody.class)) {
                    Class<?> type = param.getType();
                    if (type == String.class) {
                        args[i] = req.body();
                    } else {
                        args[i] = FileUtils.GSON.fromJson(req.body(), type);
                    }
                } else if (param.getType() == HttpRequest.class) {
                    args[i] = req;
                } else if (param.getType() == HttpResponse.class) {
                    args[i] = res;
                } else {
                    args[i] = null;
                }
            }

            method.invoke(instance, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}