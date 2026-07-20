package de.pocketcloud.shared.component.data;

import de.pocketcloud.api.server.data.ICloudServerData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
@AllArgsConstructor
public class CloudServerData implements ICloudServerData {

    protected final UUID serverUuid;
    protected final int port;
    protected int maxPlayers;

    protected Long processId = null;
    protected Long tempProcessId = null;
    protected Double tps = null;
    protected Double avgTps = null;
    protected Double memoryUsage = null;
    protected Double memoryPeak = null;
    protected Double memoryLimit = null;
    protected Double cpuUsage = null;

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