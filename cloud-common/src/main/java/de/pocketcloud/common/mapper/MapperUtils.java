package de.pocketcloud.common.mapper;

import sun.misc.Unsafe;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MapperUtils {

    private static final Map<Class<?>, Field[]> FIELD_CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<? extends MapKeyConverter<?, ?>>, MapKeyConverter<?, ?>> CONVERTER_CACHE = new ConcurrentHashMap<>();

    private static final Map<Class<?>, MapKeyConverter<?, ?>> TYPE_CONVERTERS = new ConcurrentHashMap<>();

    public static <T, R> void registerConverter(Class<T> type, MapKeyConverter<T, R> converter) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(converter, "converter must not be null");
        TYPE_CONVERTERS.put(type, converter);
    }

    public static void unregisterConverter(Class<?> type) {
        TYPE_CONVERTERS.remove(type);
    }

    @SuppressWarnings("unchecked")
    private static <T, R> MapKeyConverter<T, R> getTypeConverter(Class<?> type) {
        return (MapKeyConverter<T, R>) TYPE_CONVERTERS.get(type);
    }

    public static Map<String, Object> toMap(Object obj) {
        Objects.requireNonNull(obj, "obj must not be null");
        try {
            if (obj.getClass().isRecord()) {
                return toMapRecord(obj);
            }

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
                    MapKeyConverter<Object, Object> converter = getConverter(mapKey.converter());
                    String name = mapKey.name().isBlank() ? field.getName() : mapKey.name();
                    map.put(name, converter.toValue(value));
                } else {
                    MapKeyConverter<Object, Object> typeConverter = getTypeConverter(field.getType());
                    if (typeConverter != null) {
                        map.put(field.getName(), typeConverter.toValue(value));
                    } else {
                        map.put(field.getName(), convertToMap(value));
                    }
                }
            }
            return map;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to map object of type " + obj.getClass().getName() + " to map", e);
        }
    }

    private static Map<String, Object> toMapRecord(Object obj) {
        try {
            Map<String, Object> map = new LinkedHashMap<>();
            for (RecordComponent component : obj.getClass().getRecordComponents()) {
                var accessor = component.getAccessor();
                accessor.setAccessible(true);
                Object value = accessor.invoke(obj);

                if (value == null) {
                    map.put(component.getName(), null);
                    continue;
                }

                MapKey mapKey = component.getAnnotation(MapKey.class);
                MapInline mapInline = component.getAnnotation(MapInline.class);

                if (mapInline != null) {
                    map.putAll(toMap(value));
                } else if (mapKey != null) {
                    MapKeyConverter<Object, Object> converter = getConverter(mapKey.converter());
                    String name = mapKey.name().isBlank() ? component.getName() : mapKey.name();
                    map.put(name, converter.toValue(value));
                } else {
                    MapKeyConverter<Object, Object> typeConverter = getTypeConverter(component.getType());
                    if (typeConverter != null) {
                        map.put(component.getName(), typeConverter.toValue(value));
                    } else {
                        map.put(component.getName(), convertToMap(value));
                    }
                }
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map record of type " + obj.getClass().getName() + " to map", e);
        }
    }

    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        try {
            if (clazz.isRecord()) {
                return fromMapRecord(map, clazz);
            }

            T instance = createInstance(clazz);
            for (Field field : getFields(clazz)) {
                field.setAccessible(true);
                MapKey mapKey = field.getAnnotation(MapKey.class);
                MapInline mapInline = field.getAnnotation(MapInline.class);

                if (mapInline != null) {
                    field.set(instance, fromMap(map, field.getType()));
                } else {
                    Object value = mapKey != null ? (mapKey.name().isBlank() ? map.get(field.getName()) : map.get(mapKey.name())) : map.get(field.getName());
                    if (value == null) continue;
                    if (mapKey != null) {
                        MapKeyConverter<Object, Object> converter = getConverter(mapKey.converter());
                        field.set(instance, converter.fromValue(value));
                    } else {
                        MapKeyConverter<Object, Object> typeConverter = getTypeConverter(field.getType());
                        if (typeConverter != null) {
                            field.set(instance, typeConverter.fromValue(value));
                        } else {
                            field.set(instance, convertFromMap(value, field.getType()));
                        }
                    }
                }
            }
            return instance;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map map to object of type " + clazz.getName(), e);
        }
    }

    private static <T> T fromMapRecord(Map<String, Object> map, Class<T> clazz) throws Exception {
        RecordComponent[] components = clazz.getRecordComponents();

        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            paramTypes[i] = component.getType();

            MapKey mapKey = component.getAnnotation(MapKey.class);
            MapInline mapInline = component.getAnnotation(MapInline.class);

            if (mapInline != null) {
                args[i] = fromMap(map, component.getType());
            } else {
                Object value = mapKey != null ? (mapKey.name().isBlank() ? map.get(component.getName()) : map.get(mapKey.name())) : map.get(component.getName());
                if (mapKey != null) {
                    MapKeyConverter<Object, Object> converter = getConverter(mapKey.converter());
                    args[i] = value == null ? null : converter.fromValue(value);
                } else {
                    MapKeyConverter<Object, Object> typeConverter = getTypeConverter(component.getType());
                    if (typeConverter != null) {
                        args[i] = value == null ? null : typeConverter.fromValue(value);
                    } else {
                        args[i] = value == null ? null : convertFromMap(value, component.getType());
                    }
                }
            }
        }

        Constructor<T> constructor = clazz.getDeclaredConstructor(paramTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createInstance(Class<T> clazz) throws Exception {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException _) {}

        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);
            return (T) unsafe.allocateInstance(clazz);
        } catch (Exception _) {}

        try {
            var rf = ReflectionFactory.getReflectionFactory();
            var objConstructor = Object.class.getDeclaredConstructor();
            var syntheticConstructor = (Constructor<T>) rf.newConstructorForSerialization(clazz, objConstructor);
            return syntheticConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate " + clazz.getName() + ": no no-arg constructor, Unsafe unavailable, and ReflectionFactory failed.", e);
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
            Map<String, Field> fields = new LinkedHashMap<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field f : current.getDeclaredFields()) {
                    int mod = f.getModifiers();
                    if (!Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                        fields.putIfAbsent(f.getName(), f);
                    }
                }
                current = current.getSuperclass();
            }
            return fields.values().toArray(Field[]::new);
        });
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
            if (target == UUID.class) return UUID.fromString(s);
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
        if (value == null) return null;

        MapKeyConverter<Object, Object> typeConverter = getTypeConverter(value.getClass());
        if (typeConverter != null) {
            return typeConverter.toValue(value);
        }

        if (isPrimitive(value.getClass())) return value;

        if (value instanceof Enum<?> e) {
            return e.name();
        }

        if (value instanceof UUID uuid) {
            return uuid.toString();
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(k.toString(), convertToMap(v)));
            return result;
        }

        if (value instanceof Collection<?> col) {
            List<Object> result = new ArrayList<>();
            for (Object item : col) result.add(convertToMap(item));
            return result;
        }

        if (value.getClass().isArray()) {
            List<Object> result = new ArrayList<>();
            for (Object item : (Object[]) value) result.add(convertToMap(item));
            return result;
        }

        return toMap(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertFromMap(Object value, Class<?> targetType) {
        if (value == null) return null;

        MapKeyConverter<Object, Object> typeConverter = getTypeConverter(targetType);
        if (typeConverter != null) {
            return typeConverter.fromValue(value);
        }

        if (isPrimitive(targetType)) return castValue(value, targetType);
        if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, value.toString());

        if (targetType == List.class || targetType == ArrayList.class) {
            if (value instanceof List<?> list) return list;
            return new ArrayList<>();
        }
        if (targetType == Set.class || targetType == HashSet.class) {
            if (value instanceof Collection<?> col) return new HashSet<>(col);
            return new HashSet<>();
        }

        if (value instanceof Map<?, ?> map) {
            return fromMap((Map<String, Object>) map, targetType);
        }

        if (targetType.isInstance(value)) return value;

        return castValue(value, targetType);
    }

    private static boolean isPrimitive(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class;
    }
}