package de.pocketcloud.bridge.api;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.bridge.adapter.NativePlayerAdapter;
import de.pocketcloud.bridge.config.LocalServerConfig;

public interface IPlatformPlugin {

    void registerCommands();

    void startTask(Runnable runnable, int period);

    ILogger craftPlatformLogger();

    LocalServerConfig fetchEnvironmentConfig();

    NativePlayerAdapter<?> buildNativePlayerAdapter();

    void shutdownServer();

    double tps();

    double avgTps();

    int currentPlayers();

    int maxPlayers();
}