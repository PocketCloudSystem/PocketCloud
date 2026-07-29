package de.pocketcloud.api.event;

import de.pocketcloud.api.CloudAPI;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class EventService<R> {

    protected final Map<Class<?>, List<RegisteredHandler>> handlers = new ConcurrentHashMap<>();
    protected final Map<R, List<RegisteredHandler>> registrantHandlers = new ConcurrentHashMap<>();
    protected final Map<Listener, List<RegisteredHandler>> listenerHandlers = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T extends Event> RegisteredHandler subscribe(Class<T> eventClass, EventPriority priority, Consumer<T> handler, R registrant) {
        RegisteredHandler registeredHandler = new RegisteredHandler(eventClass, priority, (Consumer<Event>) handler);

        handlers.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                .add(registeredHandler);
        registrantHandlers.computeIfAbsent(registrant, k -> new CopyOnWriteArrayList<>())
                .add(registeredHandler);

        return registeredHandler;
    }

    public void unsubscribe(RegisteredHandler handler) {
        List<RegisteredHandler> list = handlers.get(handler.eventClass());
        if (list != null) list.remove(handler);
    }

    public void registerListener(Listener listener, R registrant) {
        Class<?> clazz = listener.getClass();
        List<RegisteredHandler> registeredHandlers = new ArrayList<>();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!isValidListenerMethod(method)) continue;

            Parameter param = method.getParameters()[0];
            Class<?> eventType = param.getType();

            if (!Event.class.isAssignableFrom(eventType)) continue;

            method.setAccessible(true);

            RegisteredHandler rh = getRegisteredHandler(listener, method, eventType);
            registeredHandlers.add(rh);

            handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                    .add(rh);
        }

        listenerHandlers.put(listener, registeredHandlers);
        registrantHandlers.computeIfAbsent(registrant, k -> new CopyOnWriteArrayList<>())
                .addAll(registeredHandlers);
    }

    public void unregisterListener(Listener listener) {
        List<RegisteredHandler> list = listenerHandlers.remove(listener);
        if (list == null) return;

        removeFromDispatch(list);
    }

    public void unregisterAll(R registrant) {
        List<RegisteredHandler> list = registrantHandlers.remove(registrant);
        if (list == null) return;

        removeFromDispatch(list);
        listenerHandlers.values().removeIf(list::containsAll);
    }

    protected void removeFromDispatch(List<RegisteredHandler> toRemove) {
        for (RegisteredHandler rh : toRemove) {
            List<RegisteredHandler> eventHandlers = handlers.get(rh.eventClass());
            if (eventHandlers != null) eventHandlers.remove(rh);
        }
    }

    public void call(Event event) {
        List<RegisteredHandler> toCall = handlers.getOrDefault(event.getClass(), new ArrayList<>());
        toCall.sort(Comparator.comparingInt(rh -> rh.priority().ordinal()));
        for (RegisteredHandler handler : toCall) {
            try {
                handler.handler().accept(event);
            } catch (Throwable e) {
                CloudAPI.instance().logger().exception("§cException caught during event handling of {}", e, event.getClass().getName());
            }
        }
    }

    protected RegisteredHandler getRegisteredHandler(Listener listener, Method method, Class<?> eventClass) {
        EventPriority priority = method.isAnnotationPresent(EventHandler.class) ? method.getAnnotation(EventHandler.class).priority() : EventPriority.NORMAL;
        return new RegisteredHandler(eventClass, priority, event -> {
            try {
                method.invoke(listener, event);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke listener method " + method.getName(), e);
            }
        });
    }

    protected boolean isValidListenerMethod(Method method) {
        return method.getParameterCount() == 1 && !Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers());
    }
}