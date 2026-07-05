package de.pocketcloud.common.mapper;

public interface MapKeyConverter<T, R> {

    R toValue(T obj);
    T fromValue(R value);
}