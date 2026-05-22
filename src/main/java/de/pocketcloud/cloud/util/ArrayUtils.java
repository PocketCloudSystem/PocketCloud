package de.pocketcloud.cloud.util;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ArrayUtils {

    public static boolean hasAllKeys(Map<String, Object> map, Map<String, Object> defaultMap) {
        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();

            if (!map.containsKey(key)) return false;

            if (defaultValue instanceof Map<?, ?> defaultNested && !((Map<?, ?>) defaultValue).isEmpty()) {
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

    public static Map<String, Object> fillMissingKeys(Map<String, Object> map, Map<String, Object> defaultMap) {
        return fillMissingKeys(map, defaultMap, null, false);
    }

    public static Map<String, Object> fillMissingKeys(Map<String, Object> map, Map<String, Object> defaultMap, boolean enforceTypes) {
        return fillMissingKeys(map, defaultMap, null, enforceTypes);
    }

    public static Map<String, Object> fillMissingKeys(Map<String, Object> map, Map<String, Object> defaultMap, AtomicInteger affectedKeys, boolean enforceTypes) {
        if (affectedKeys == null) affectedKeys = new AtomicInteger(0);

        for (Map.Entry<String, Object> entry : defaultMap.entrySet()) {
            String key = entry.getKey();
            Object defaultValue = entry.getValue();
            Object actualValue = map.get(key);

            if (!map.containsKey(key)) {
                affectedKeys.incrementAndGet();
                map.put(key, defaultValue);
            } else if (defaultValue instanceof Map<?, ?> && actualValue instanceof Map<?, ?>) {
                affectedKeys.incrementAndGet();
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) actualValue;
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedDefault = (Map<String, Object>) defaultValue;

                map.put(key, fillMissingKeys(nestedMap, nestedDefault, affectedKeys, enforceTypes));
            } else if (defaultValue instanceof Map<?, ?> && !(actualValue instanceof Map<?, ?>)) {
                affectedKeys.incrementAndGet();
                map.put(key, defaultValue);
            } else if (enforceTypes && defaultValue != null && actualValue != null) {
                if (!defaultValue.getClass().equals(actualValue.getClass())) {
                    affectedKeys.incrementAndGet();
                    map.put(key, defaultValue);
                }
            }
        }
        return map;
    }
}