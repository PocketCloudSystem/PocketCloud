package de.pocketcloud.bridge.api;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.bridge.config.LocalServerConfig;
import de.pocketcloud.common.config.exception.UnsupportedFileExtensionException;

import java.io.IOException;

public interface IPlatformPlugin {

    void onVerification();

    void startTask(Runnable runnable, int period);

    ILogger craftPlatformLogger();

    LocalServerConfig fetchEnvironmentConfig() throws UnsupportedFileExtensionException, IOException;

    NativePlayerAdapter<?> buildNativePlayerAdapter();

    void shutdownServer();

    double tps();

    double avgTps();

    int currentPlayers();

    int maxPlayers();
}