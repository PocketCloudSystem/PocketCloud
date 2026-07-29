package de.pocketcloud.common.serialization.annotation;

public interface MapKeyConverter<T, R> {

    R toValue(T obj);
    T fromValue(R value);
}