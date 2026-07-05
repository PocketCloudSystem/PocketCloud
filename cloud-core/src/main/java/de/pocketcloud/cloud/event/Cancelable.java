package de.pocketcloud.cloud.event;

public interface Cancelable {

    void cancel();

    void uncancel();

    boolean isCancelled();
}