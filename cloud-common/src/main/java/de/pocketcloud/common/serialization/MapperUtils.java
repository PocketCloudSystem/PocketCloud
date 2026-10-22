package de.pocketcloud.common.serialization;

import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapInline;
import de.pocketcloud.common.serialization.annotation.MapKey;
import de.pocketcloud.common.serialization.annotation.MapKeyConverter;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reflection based Object <-> Map mapper.
 * <p>
 * Field access happens exclusively via {@link VarHandle}s (no {@code setAccessible}, no {@code Unsafe}).
 * Consequently {@code final} fields can only be populated through a {@link MapCreator} constructor
 * (or the canonical constructor of a record).
 */
public final class MapperUtils {

    private static final Map<Class<?>, List<Property>> PROPERTY_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Optional<Constructor<?>>> CREATOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<Constructor<?>, MethodHandle> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends MapKeyConverter<?, ?>>, MapKeyConverter<?, ?>> CONVERTER_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, MapKeyConverter<?, ?>> TYPE_CONVERTERS = new ConcurrentHashMap<>();

    private MapperUtils() {}

    public static <T, R> void registerConverter(Class<T> type, MapKeyConverter<T, R> converter) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(converter, "converter must not be null");
        TYPE_CONVERTERS.put(type, converter);
    }

    public static void unregisterConverter(Class<?> type) {
        TYPE_CONVERTERS.remove(type);
    }

    public static Map<String, Object> toMap(Object obj) {
        Objects.requireNonNull(obj, "obj must not be null");

        Map<String, Object> map = new LinkedHashMap<>();
        for (Property property : properties(obj.getClass())) {
            Object value = property.get(obj);

            if (value == null) {
                map.put(property.key(), null);
            } else if (property.inline()) {
                map.putAll(toMap(value));
            } else {
                map.put(property.key(), writeValue(property, value));
            }
        }
        return map;
    }

    public static <T> T fromMap(Map<String, Object> map, Class<T> clazz) {
        Objects.requireNonNull(map, "map must not be null");
        Objects.requireNonNull(clazz, "clazz must not be null");
        try {
            return clazz.isRecord() ? fromMapRecord(map, clazz) : fromMapObject(map, clazz);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map map to object of type " + clazz.getName(), e);
        }
    }

    /**
     * Converts a serialized value back to the given target type,
     * using the registered type converters.
     */
    public static Object convertValue(Object value, Class<?> targetType) {
        Objects.requireNonNull(targetType, "targetType must not be null");
        return convertFromMap(value, targetType);
    }

    private static <T> T fromMapObject(Map<String, Object> map, Class<T> clazz) {
        Set<String> handledKeys = new HashSet<>();
        Constructor<T> creator = findMapCreator(clazz);
        T instance = creator != null ? invokeMapCreator(map, creator, handledKeys) : createInstance(clazz);

        for (Property property : properties(clazz)) {
            if (handledKeys.contains(property.key())) continue;

            if (property.inline()) {
                if (property.writable()) property.set(instance, fromMap(map, property.type()));
                continue;
            }

            Object value = map.get(property.key());
            if (value == null) continue;

            if (!property.writable()) {
                throw new IllegalStateException("Cannot set final field '" + property.fieldName() + "' of " + clazz.getName()
                        + ". Final fields must be provided through a @MapCreator constructor.");
            }
            property.set(instance, readValue(property, value));
        }
        return instance;
    }

    private static <T> T fromMapRecord(Map<String, Object> map, Class<T> clazz) {
        List<Property> properties = properties(clazz);
        Class<?>[] paramTypes = new Class<?>[properties.size()];
        Object[] args = new Object[properties.size()];

        for (int i = 0; i < args.length; i++) {
            Property property = properties.get(i);
            paramTypes[i] = property.type();

            if (property.inline()) {
                args[i] = fromMap(map, property.type());
                continue;
            }

            Object value = map.get(property.key());
            args[i] = value == null ? defaultValue(property.type()) : readValue(property, value);
        }

        try {
            return construct(clazz.getDeclaredConstructor(paramTypes), args);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Canonical constructor of record " + clazz.getName() + " not found", e);
        }
    }

    private static <T> T invokeMapCreator(Map<String, Object> map, Constructor<T> constructor, Set<String> handledKeys) {
        Parameter[] parameters = constructor.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            MapKey mapKey = parameter.getAnnotation(MapKey.class);

            if (mapKey == null || mapKey.name().isBlank()) {
                throw new IllegalStateException(
                        "Missing @MapKey(name = \"...\") on parameter " + i + " of @MapCreator constructor in "
                                + constructor.getDeclaringClass().getName()
                                + ". Since the project is not compiled with -parameters, every parameter must explicitly "
                                + "declare the map key it corresponds to.");
            }

            handledKeys.add(mapKey.name());

            Object value = map.get(mapKey.name());
            if (value == null) {
                args[i] = defaultValue(parameter.getType());
                continue;
            }

            if (hasConverter(mapKey)) {
                MapKeyConverter<Object, Object> converter = getConverter(mapKey.converter());
                args[i] = converter.fromValue(value);
            } else {
                Class<?> targetType = mapKey.impl() != Void.class ? mapKey.impl() : parameter.getType();
                args[i] = convertFromMap(value, targetType);
            }
        }

        return construct(constructor, args);
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> findMapCreator(Class<T> clazz) {
        return (Constructor<T>) CREATOR_CACHE.computeIfAbsent(clazz, c ->
                Arrays.stream(c.getDeclaredConstructors())
                        .filter(constructor -> constructor.isAnnotationPresent(MapCreator.class))
                        .findFirst()
                        .map(constructor -> (Constructor<?>) constructor)
        ).orElse(null);
    }

    /**
     * Creates an instance without a {@link MapCreator}: uses the no-arg constructor if present,
     * otherwise the constructor with the fewest parameters, filled with default values.
     */
    private static <T> T createInstance(Class<T> clazz) {
        if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalArgumentException("Cannot instantiate abstract type " + clazz.getName());
        }

        Constructor<T> constructor;
        try {
            constructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            constructor = smallestConstructor(clazz);
        }

        Class<?>[] types = constructor.getParameterTypes();
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            args[i] = defaultValue(types[i]);
        }

        try {
            return construct(constructor, args);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Cannot instantiate " + clazz.getName() + " via " + constructor
                    + " with default arguments. Add a no-arg constructor or a @MapCreator constructor.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> smallestConstructor(Class<T> clazz) {
        return (Constructor<T>) Arrays.stream(clazz.getDeclaredConstructors())
                .min(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow(() -> new IllegalStateException("No constructor found for " + clazz.getName()));
    }

    @SuppressWarnings("unchecked")
    private static <T> T construct(Constructor<T> constructor, Object... args) {
        MethodHandle handle = CONSTRUCTOR_CACHE.computeIfAbsent(constructor, c -> {
            try {
                return MethodHandles.privateLookupIn(c.getDeclaringClass(), MethodHandles.lookup()).unreflectConstructor(c);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("No access to constructor " + c, e);
            }
        });

        try {
            return (T) handle.invokeWithArguments(args);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke constructor " + constructor, t);
        }
    }

    private static Object defaultValue(Class<?> type) {
        return type.isPrimitive() ? Array.get(Array.newInstance(type, 1), 0) : null;
    }

    // Property metadata (VarHandle based field access)

    private record Property(
            String fieldName,
            String key,
            Class<?> type,
            Class<?> targetType,
            Class<? extends MapKeyConverter<?, ?>> converter,
            boolean inline,
            boolean writable,
            VarHandle handle
    ) {
        Object get(Object owner) {
            return handle.get(owner);
        }

        void set(Object owner, Object value) {
            try {
                handle.set(owner, value);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Cannot assign " + (value == null ? "null" : value.getClass().getName())
                        + " to " + type.getName() + " (property '" + key + "')", e);
            }
        }
    }

    private static List<Property> properties(Class<?> clazz) {
        return PROPERTY_CACHE.computeIfAbsent(clazz, MapperUtils::scanProperties);
    }

    private static List<Property> scanProperties(Class<?> clazz) {
        try {
            return clazz.isRecord() ? scanRecord(clazz) : scanClass(clazz);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot access properties of " + clazz.getName(), e);
        }
    }

    private static List<Property> scanClass(Class<?> clazz) throws ReflectiveOperationException {
        Map<String, Property> result = new LinkedHashMap<>();

        for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
            Lookup lookup = MethodHandles.privateLookupIn(current, MethodHandles.lookup());

            for (Field field : current.getDeclaredFields()) {
                int mod = field.getModifiers();
                if (Modifier.isStatic(mod) || Modifier.isTransient(mod) || field.isSynthetic()) continue;
                if (result.containsKey(field.getName())) continue;

                VarHandle handle = lookup.findVarHandle(current, field.getName(), field.getType());
                result.put(field.getName(), createProperty(
                        field.getName(), field.getType(),
                        field.getAnnotation(MapKey.class), field.getAnnotation(MapInline.class),
                        !Modifier.isFinal(mod), handle));
            }
        }
        return List.copyOf(result.values());
    }

    private static List<Property> scanRecord(Class<?> clazz) throws ReflectiveOperationException {
        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        List<Property> result = new ArrayList<>();

        for (RecordComponent component : clazz.getRecordComponents()) {
            VarHandle handle = lookup.findVarHandle(clazz, component.getName(), component.getType());
            result.add(createProperty(
                    component.getName(), component.getType(),
                    component.getAnnotation(MapKey.class), component.getAnnotation(MapInline.class),
                    false, handle));
        }
        return List.copyOf(result);
    }

    private static Property createProperty(String fieldName, Class<?> type, MapKey mapKey, MapInline mapInline,
                                           boolean writable, VarHandle handle) {
        String key = mapKey != null && !mapKey.name().isBlank() ? mapKey.name() : fieldName;
        Class<?> targetType = mapKey != null && mapKey.impl() != Void.class ? mapKey.impl() : type;
        Class<? extends MapKeyConverter<?, ?>> converter = mapKey != null && hasConverter(mapKey) ? mapKey.converter() : null;
        return new Property(fieldName, key, type, targetType, converter, mapInline != null, writable, handle);
    }

    private static boolean hasConverter(MapKey mapKey) {
        return mapKey.converter() != MapKey.NoConverter.class;
    }

    @SuppressWarnings("unchecked")
    private static <T, R> MapKeyConverter<T, R> getTypeConverter(Class<?> type) {
        return (MapKeyConverter<T, R>) TYPE_CONVERTERS.get(type);
    }

    @SuppressWarnings("unchecked")
    private static <T, R> MapKeyConverter<T, R> getConverter(Class<? extends MapKeyConverter<?, ?>> clazz) {
        return (MapKeyConverter<T, R>) CONVERTER_CACHE.computeIfAbsent(clazz, c -> {
            try {
                return construct(c.getDeclaredConstructor());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Converter " + c.getName() + " needs a no-arg constructor", e);
            }
        });
    }

    /** Property value -> map value */
    private static Object writeValue(Property property, Object value) {
        if (property.converter() != null) {
            MapKeyConverter<Object, Object> converter = getConverter(property.converter());
            return converter.toValue(value);
        }
        MapKeyConverter<Object, Object> typeConverter = getTypeConverter(property.type());
        return typeConverter != null ? typeConverter.toValue(value) : convertToMap(value);
    }

    /** Map value -> property value */
    private static Object readValue(Property property, Object value) {
        if (property.converter() != null) {
            MapKeyConverter<Object, Object> converter = getConverter(property.converter());
            return converter.fromValue(value);
        }
        return convertFromMap(value, property.targetType());
    }

    private static Object convertToMap(Object value) {
        if (value == null) return null;

        MapKeyConverter<Object, Object> typeConverter = getTypeConverter(value.getClass());
        if (typeConverter != null) return typeConverter.toValue(value);

        if (isSimpleType(value.getClass())) return value;
        if (value instanceof Enum<?> e) return e.name();
        if (value instanceof UUID uuid) return uuid.toString();

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(k.toString(), convertToMap(v)));
            return result;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> result = new ArrayList<>(collection.size());
            for (Object item : collection) result.add(convertToMap(item));
            return result;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> result = new ArrayList<>(length);
            for (int i = 0; i < length; i++) result.add(convertToMap(Array.get(value, i)));
            return result;
        }

        return toMap(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convertFromMap(Object value, Class<?> targetType) {
        if (value == null) return null;

        MapKeyConverter<Object, Object> typeConverter = getTypeConverter(targetType);
        if (typeConverter != null) return typeConverter.fromValue(value);

        if (isSimpleType(targetType)) return castValue(value, targetType);
        if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, value.toString());

        if (targetType.isArray() && value instanceof Collection<?> collection) {
            Class<?> componentType = targetType.getComponentType();
            Object array = Array.newInstance(componentType, collection.size());
            int i = 0;
            for (Object item : collection) {
                Array.set(array, i++, convertFromMap(item, componentType));
            }
            return array;
        }

        if (targetType == List.class || targetType == ArrayList.class) {
            return value instanceof List<?> list ? list : new ArrayList<>();
        }

        if (targetType == Set.class || targetType == HashSet.class) {
            return value instanceof Collection<?> collection ? new HashSet<>(collection) : new HashSet<>();
        }

        if (Map.class.isAssignableFrom(targetType)) {
            return value instanceof Map<?, ?> source ? copyMap(source, targetType) : new LinkedHashMap<>();
        }

        if (value instanceof Map<?, ?> map) {
            return fromMap((Map<String, Object>) map, targetType);
        }

        if (targetType.isInstance(value)) return value;

        return castValue(value, targetType);
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> copyMap(Map<?, ?> source, Class<?> targetType) {
        Map<Object, Object> target;
        if (targetType.isInterface() || Modifier.isAbstract(targetType.getModifiers())) {
            target = new LinkedHashMap<>();
        } else {
            try {
                target = (Map<Object, Object>) createInstance(targetType);
            } catch (RuntimeException e) {
                target = new LinkedHashMap<>();
            }
        }
        target.putAll(source);
        return target;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object castValue(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) return value;

        if (target.isEnum()) return Enum.valueOf((Class<Enum>) target, value.toString());

        if (value instanceof Number n) {
            if (target == byte.class || target == Byte.class) return n.byteValue();
            if (target == short.class || target == Short.class) return n.shortValue();
            if (target == int.class || target == Integer.class) return n.intValue();
            if (target == long.class || target == Long.class) return n.longValue();
            if (target == float.class || target == Float.class) return n.floatValue();
            if (target == double.class || target == Double.class) return n.doubleValue();
            if (target == boolean.class || target == Boolean.class) return n.intValue() != 0;
            if (target == char.class || target == Character.class) return (char) n.intValue();
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

        return value;
    }

    private static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Boolean.class
                || type == Character.class
                || Number.class.isAssignableFrom(type);
    }
}