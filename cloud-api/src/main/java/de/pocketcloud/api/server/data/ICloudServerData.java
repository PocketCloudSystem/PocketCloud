package de.pocketcloud.api.server.data;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ICloudServerData {

    UUID serverUuid();

    String address();

    int port();

    int maxPlayers();

    @Nullable Long processId();

    @Nullable Double tps();

    @Nullable Double avgTps();

    @Nullable Long memoryUsage();

    @Nullable Long memoryPeak();

    @Nullable Long memoryLimit();

    @Nullable Double cpuUsage();

    ICloudServerData processId(@Nullable Long processId);

    ICloudServerData tps(@Nullable Double tps);

    ICloudServerData avgTps(@Nullable Double avgTps);

    ICloudServerData memoryUsage(@Nullable Long memoryUsage);

    ICloudServerData memoryPeak(@Nullable Long memoryPeak);

    ICloudServerData memoryLimit(@Nullable Long memoryLimit);

    ICloudServerData cpuUsage(@Nullable Double cpuUsage);
}