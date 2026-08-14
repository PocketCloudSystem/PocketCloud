package de.pocketcloud.common.serialization.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
public @interface MapKey {

    String name() default "";

    /**
     * Optional converter used to transform the value when mapping to/from the map.
     * Leave unset if you only want to override {@link #name()} and/or {@link #impl()}.
     */
    Class<? extends MapKeyConverter<?, ?>> converter() default NoConverter.class;

    /**
     * Overrides the declared field/component/parameter type with a concrete implementation
     * to instantiate when deserializing (e.g. an abstract type -> concrete subclass).
     * Has no effect on serialization, since the actual runtime type of the value is used there.
     */
    Class<?> impl() default Void.class;

    /**
     * Sentinel used as the default for {@link #converter()} to signal "no converter set".
     * Never actually invoked.
     */
    final class NoConverter implements MapKeyConverter<Object, Object> {
        @Override
        public Object toValue(Object obj) {
            throw new UnsupportedOperationException("NoConverter must never be invoked");
        }

        @Override
        public Object fromValue(Object value) {
            throw new UnsupportedOperationException("NoConverter must never be invoked");
        }
    }
}