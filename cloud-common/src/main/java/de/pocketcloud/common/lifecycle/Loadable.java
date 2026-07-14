package de.pocketcloud.common.lifecycle;

public interface Loadable {

    default void preload() {}

    void load();

    void unload();
}