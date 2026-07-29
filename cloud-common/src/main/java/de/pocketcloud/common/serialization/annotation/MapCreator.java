package de.pocketcloud.common.serialization.annotation;

import de.pocketcloud.common.serialization.MapperUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a constructor that {@link MapperUtils#fromMap(java.util.Map, Class)} should use to
 * instantiate the target class, instead of the default no-arg-constructor/Unsafe fallback.
 * <p>
 * Only constructors declared directly on the requested class are considered (like Jackson's
 * {@code @JsonCreator}). Since the project is not compiled with {@code -parameters}, every
 * parameter of the annotated constructor must carry {@link MapKey#name()} explicitly (this
 * is what gets matched against the map key, and against the corresponding field to avoid
 * setting it twice). Additionally supports a {@link MapKeyConverter} and/or an
 * {@link MapKey#impl()} override per parameter.
 * <p>
 * Any fields not covered by the annotated constructor's parameters are set afterwards using
 * the normal field-based mapping.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface MapCreator {}
