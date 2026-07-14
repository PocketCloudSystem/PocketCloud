package de.pocketcloud.cloud.server.util;

import de.pocketcloud.api.server.data.ICloudServerData;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
public final class CloudServerData implements ICloudServerData {

    private final UUID serverUuid;
    private final int port;
    private int maxPlayers;

    private Long processId = null;
    private Long tempProcessId = null;
    private Double tps = null;
    private Double avgTps = null;
    private Double memoryUsage = null;
    private Double memoryPeak = null;
    private Double memoryLimit = null;
    private Double cpuUsage = null;

    public CloudServerData(UUID serverUuid, int port, int maxPlayers) {
        this.serverUuid = serverUuid;
        this.port = port;
        this.maxPlayers = maxPlayers;
    }

    public void setPerformanceStats(Double tps, Double avgTps, Double memoryUsage, Double memoryPeak, Double memoryLimit, Double cpuUsage) {
        this.tps = tps;
        this.avgTps = avgTps;
        this.memoryUsage = memoryUsage;
        this.memoryPeak = memoryPeak;
        this.memoryLimit = memoryLimit;
        this.cpuUsage = cpuUsage;
    }
}