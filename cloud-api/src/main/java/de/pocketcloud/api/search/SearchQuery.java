package de.pocketcloud.api.search;

public interface SearchQuery<T> {

    boolean matches(T object);
}