package de.pocketcloud.api.server.data;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface ICloudServerData {

    UUID serverUuid();

    String address();

    int port();

    int maxPlayers();

    @Nullable
    default Long usableProcessId() {
        return processId() != null ? processId() : tempProcessId();
    }

    @Nullable Long processId();

    @Nullable Long tempProcessId();

    Double tps();

    Double avgTps();

    Long memoryUsage();

    Long memoryPeak();

    Long memoryLimit();

    Double cpuUsage();

    ICloudServerData processId(Long processId);

    ICloudServerData tempProcessId(Long tempProcessId);

    ICloudServerData tps(Double tps);

    ICloudServerData avgTps(Double avgTps);

    ICloudServerData memoryUsage(Long memoryUsage);

    ICloudServerData memoryPeak(Long memoryPeak);

    ICloudServerData memoryLimit(Long memoryLimit);

    ICloudServerData cpuUsage(Double cpuUsage);
}