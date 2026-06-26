package de.pocketcloud.cloud.util;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ArrayUtils {

    public static LinkedHashMap<String, Object> orderedMap(Object... keysAndValues) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    public static boolean hasAllKeys(Map<String, Object> map, Map<String, Object> defaultMap) {
        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (!map.containsKey(key)) return false;

            if (defaultValue instanceof Map<?, ?> defaultNested && !defaultNested.isEmpty()) {
                Object actual = map.get(key);
                if (!(actual instanceof Map<?, ?>)) return false;

                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) actual;
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedDefault = (Map<String, Object>) defaultNested;

                if (!hasAllKeys(nestedMap, nestedDefault)) return false;
            }
        }
        return true;
    }

    public static <K, V> Map<K, V> fillMissingKeys(Map<K, V> map, Map<K, V> defaultMap) {
        return fillMissingKeys(map, defaultMap, null, false);
    }

    public static <K, V> Map<K, V> fillMissingKeys(Map<K, V> map, Map<K, V> defaultMap, boolean enforceTypes) {
        return fillMissingKeys(map, defaultMap, null, enforceTypes);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> fillMissingKeys(Map<K, V> map, Map<K, V> defaultMap, @Nullable AtomicInteger affectedKeys, boolean enforceTypes) {
        if (affectedKeys == null) affectedKeys = new AtomicInteger(0);
        for (Map.Entry<K, V> entry : defaultMap.entrySet()) {
            K key = entry.getKey();
            V defaultValue = entry.getValue();
            V actualValue = map.get(key);

            if (!map.containsKey(key)) {
                affectedKeys.incrementAndGet();
                map.put(key, defaultValue);
            } else if (defaultValue instanceof Map<?, ?> defaultNested && actualValue instanceof Map<?, ?> actualNested) {
                affectedKeys.incrementAndGet();
                Map<Object, Object> merged = fillMissingKeys((Map<Object, Object>) actualNested, (Map<Object, Object>) defaultNested, affectedKeys, enforceTypes);
                map.put(key, (V) merged);
            } else if (defaultValue instanceof Map<?, ?>) {
                affectedKeys.incrementAndGet();
                map.put(key, defaultValue);
            } else if (enforceTypes && defaultValue != null && actualValue != null && !defaultValue.getClass().equals(actualValue.getClass())) {
                affectedKeys.incrementAndGet();
                map.put(key, defaultValue);
            }
        }

        return map;
    }
}