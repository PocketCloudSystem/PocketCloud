package de.pocketcloud.api.service;

import java.util.HashMap;
import java.util.Map;

public final class ServiceRegistry {

    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> T register(Class<T> serviceClass, T serviceInstance) {
        if (services.containsKey(serviceClass)) throw new IllegalStateException("Service " + serviceClass.getName() + " is already registered!");
        this.services.put(serviceClass, serviceInstance);
        return serviceInstance;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> serviceClass) {
        T service = (T) this.services.get(serviceClass);
        if (service == null) throw new NullPointerException("No service registered for class " + serviceClass.getName());
        return service;
    }

    public Map<Class<?>, Object> getAll() {
        return Map.copyOf(this.services);
    }
}