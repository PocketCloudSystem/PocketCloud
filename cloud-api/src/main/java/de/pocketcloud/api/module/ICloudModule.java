package de.pocketcloud.api.module;

public interface ICloudModule {

    String id();

    String name();

    String version();

    String description();

    String[] authors();

    default void onLoad() {}

    default void onEnable() {}

    default void onDisable() {}
}