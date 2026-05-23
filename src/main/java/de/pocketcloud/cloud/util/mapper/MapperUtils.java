package de.pocketcloud.cloud.util.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MapperUtils {

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<? extends MapKeyConverter<?, ?>>, MapKeyConverter<?, ?>> CONVERTER_CACHE = new ConcurrentHashMap<>();

    public static Map<String, Object> toMap(Object obj) {
        Objects.requireNonNull(obj, "obj must not be null");
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Field field : getFields(obj.getClass())) {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value == null) {
                    map.put(field.getName(), null);
                    continue;
                }

                MapKey mapKey = field.getAnnotation(MapKey.class);
                MapInline mapInline = field.getAnnotation(MapInline.class);
                if (mapInline != null) {
                    map.putAll(toMap(value));
                } else if (mapKey != null) {
                    MapKeyConverter converter = getConverter(mapKey.converter());
                    map.put(field.getName(), converter.toValue(value));
                } else {
                    map.put(field.getName(), convertToMap(value));
                }
            }
            return map;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to map object of type " + obj.getClass().getName() + " to map", e);
        }
    }

    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        try {
            T instance = createInstance(clazz);
            for (Field field : getFields(clazz)) {
                Object value = map.get(field.getName());
                if (value == null) continue;
                field.setAccessible(true);

                MapKey mapKey = field.getAnnotation(MapKey.class);
                MapInline mapInline = field.getAnnotation(MapInline.class);

                if (mapInline != null) {
                    field.set(instance, fromMap(map, field.getType()));
                } else if (mapKey != null) {
                    MapKeyConverter converter = getConverter(mapKey.converter());
                    field.set(instance, converter.fromValue(value));
                } else {
                    field.set(instance, convertFromMap(value, field.getType()));
                }
            }
            return instance;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map map to object of type " + clazz.getName(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, R> MapKeyConverter<T, R> getConverter(Class<? extends MapKeyConverter<?, ?>> clazz) {
        return (MapKeyConverter<T, R>) CONVERTER_CACHE.computeIfAbsent(clazz, c -> {
            try {
                var constructor = c.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate converter: " + c.getName(), e);
            }
        });
    }

    private static Field[] getFields(Class<?> clazz) {
        return FIELD_CACHE.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                        fields.add(f);
                    }
                }
                current = current.getSuperclass();
            }
            return fields.toArray(Field[]::new);
        });
    }

    private static <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No no-arg constructor found for " + clazz.getName() + ". Add a private no-arg constructor or use a custom factory.", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object castValue(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) return value;

        if (target.isEnum()) {
            return Enum.valueOf((Class<Enum>) target, value.toString());
        }

        if (value instanceof Number n) {
            if (target == byte.class || target == Byte.class) return n.byteValue();
            if (target == short.class || target == Short.class) return n.shortValue();
            if (target == int.class || target == Integer.class) return n.intValue();
            if (target == long.class || target == Long.class) return n.longValue();
            if (target == float.class || target == Float.class) return n.floatValue();
            if (target == double.class || target == Double.class) return n.doubleValue();
        }

        if (value instanceof String s) {
            if (target == boolean.class || target == Boolean.class) return Boolean.parseBoolean(s);
            if (target == byte.class || target == Byte.class) return Byte.parseByte(s);
            if (target == short.class || target == Short.class) return Short.parseShort(s);
            if (target == int.class || target == Integer.class) return Integer.parseInt(s);
            if (target == long.class || target == Long.class) return Long.parseLong(s);
            if (target == float.class || target == Float.class) return Float.parseFloat(s);
            if (target == double.class || target == Double.class) return Double.parseDouble(s);
            if (target == char.class || target == Character.class) return s.isEmpty() ? '\0' : s.charAt(0);
            if (target.isEnum()) return Enum.valueOf((Class<Enum>) target, s);
        }

        if (target == boolean.class || target == Boolean.class) {
            if (value instanceof Number n) return n.intValue() != 0;
        }

        if (target == char.class || target == Character.class) {
            if (value instanceof Number n) return (char) n.intValue();
        }

        return value;
    }

    private static Object convertToMap(Object value) {
        if (isPrimitive(value.getClass())) return value;
        if (value instanceof Map) return value;
        return toMap(value);
    }

    @SuppressWarnings("unchecked")
    private static Object convertFromMap(Object value, Class<?> targetType) {
        if (isPrimitive(targetType)) return castValue(value, targetType);
        if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, value.toString());
        if (value instanceof Map) return fromMap((Map<String, Object>) value, targetType);
        return value;
    }

    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type.isEnum();
    }
}