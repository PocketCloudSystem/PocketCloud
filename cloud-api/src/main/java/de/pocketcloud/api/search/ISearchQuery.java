package de.pocketcloud.api.search;

public interface ISearchQuery<T> {

    boolean matches(T object);
}