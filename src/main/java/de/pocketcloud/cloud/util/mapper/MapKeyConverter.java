package de.pocketcloud.cloud.util.mapper;

public interface MapKeyConverter<T, R> {

    R toValue(T obj);
    T fromValue(R value);
}