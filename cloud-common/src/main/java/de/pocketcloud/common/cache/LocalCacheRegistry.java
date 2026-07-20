package de.pocketcloud.common.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LocalCacheRegistry {

    private static final Map<Class<?>, LocalCache<?, ?>> caches = new ConcurrentHashMap<>();

    public static <C extends LocalCache<?, ?>> void register(Class<C> clazz, C instance) {
        caches.put(clazz, instance);
    }

    @SuppressWarnings("unchecked")
    public static <C extends LocalCache<?, ?>> C get(Class<C> clazz) {
        return (C) caches.computeIfAbsent(clazz, _ -> {
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}