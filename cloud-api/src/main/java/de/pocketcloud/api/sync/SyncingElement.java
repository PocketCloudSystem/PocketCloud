package de.pocketcloud.api.sync;

public interface SyncingElement<T> {

    void syncIn(T data);

    void syncOut();
}