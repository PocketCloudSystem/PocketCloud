package de.pocketcloud.cloud.event;

import de.pocketcloud.cloud.plugin.CloudPlugin;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Getter
public final class EventManager {

    @Getter
    private static EventManager instance;

    private final Map<String, Map<Class<?>, List<RegisteredHandler>>> handlers = new ConcurrentHashMap<>();

    public EventManager() {
        instance = this;
    }

    public <T extends Event> void register(Class<T> eventClass, EventPriority priority, Consumer<T> handler, CloudPlugin plugin) {
        handlers.computeIfAbsent(plugin.getDescription().getName(), k -> new ConcurrentHashMap<>())
                .computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                .add(new RegisteredHandler(priority, (Consumer<Event>) handler));
    }

    public void registerListener(Listener listener, CloudPlugin plugin) {
        Class<?> clazz = listener.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (!isValidListenerMethod(method)) continue;

            Parameter param = method.getParameters()[0];
            Class<?> eventType = param.getType();

            if (!Event.class.isAssignableFrom(eventType)) continue;

            method.setAccessible(true);

            RegisteredHandler rh = getRegisteredHandler(listener, method);

            handlers.computeIfAbsent(plugin.getDescription().getName(), k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(rh);
        }
    }

    private static RegisteredHandler getRegisteredHandler(Listener listener, Method method) {
        EventPriority priority = method.isAnnotationPresent(EventHandler.class) ? method.getAnnotation(EventHandler.class).priority() : EventPriority.NORMAL;
        return new RegisteredHandler(priority, event -> {
            try {
                method.invoke(listener, event);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke listener method " + method.getName(), e);
            }
        });
    }

    private boolean isValidListenerMethod(Method method) {
        return method.getParameterCount() == 1 && !Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers());
    }

    public void removeHandlers(CloudPlugin plugin) {
        handlers.remove(plugin.getDescription().getName());
    }

    public void removeAll() {
        handlers.clear();
    }

    public void call(Event event) {
        Benchmark.startTiming("event_" + event.getName());
        List<RegisteredHandler> toCall = new ArrayList<>();

        for (Map<Class<?>, List<RegisteredHandler>> pluginMap : handlers.values()) {
            List<RegisteredHandler> list = pluginMap.get(event.getClass());
            if (list == null) continue;
            toCall.addAll(list);
        }

        toCall.sort(Comparator.comparingInt(rh -> rh.priority().ordinal()));

        for (RegisteredHandler rh : toCall) {
            rh.handler().accept(event);
        }

        Benchmark.stopTiming("event_" + event.getName());
    }
}