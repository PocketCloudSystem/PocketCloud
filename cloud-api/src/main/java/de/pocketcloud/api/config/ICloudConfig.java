package de.pocketcloud.api.config;

public interface ICloudConfig {

    void validate();

    default void apply() {}
}