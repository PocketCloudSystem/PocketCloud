package de.pocketcloud.api;

import lombok.Getter;

public final class CloudAPIHolder {

    @Getter
    private static CloudAPI instance;

    public static void setInstance(CloudAPI api) {
        if (instance != null) throw new IllegalStateException("CloudAPI instance is already set");
        instance = api;
    }
}