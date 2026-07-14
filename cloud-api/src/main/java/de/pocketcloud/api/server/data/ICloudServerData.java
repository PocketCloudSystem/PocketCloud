package de.pocketcloud.api.server.data;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ICloudServerData {

    UUID serverUuid();

    int port();

    int maxPlayers();

    @Nullable
    default Long usableProcessId() {
        return processId() != null ? processId() : tempProcessId();
    }

    @Nullable Long processId();

    @Nullable Long tempProcessId();

    @Nullable Double tps();

    @Nullable Double avgTps();

    @Nullable Double memoryUsage();

    @Nullable Double memoryPeak();

    @Nullable Double memoryLimit();

    @Nullable Double cpuUsage();

    ICloudServerData processId(Long processId);

    ICloudServerData tempProcessId(Long tempProcessId);

    ICloudServerData tps(Double tps);

    ICloudServerData avgTps(Double avgTps);

    ICloudServerData memoryUsage(Double memoryUsage);

    ICloudServerData memoryPeak(Double memoryPeak);

    ICloudServerData memoryLimit(Double memoryLimit);

    ICloudServerData cpuUsage(Double cpuUsage);
}