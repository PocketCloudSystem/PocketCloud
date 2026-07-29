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

    protected Long processId = -1L;
    protected Long tempProcessId = -1L;
    protected Double tps = 0.0;
    protected Double avgTps = 0.0;
    protected Long memoryUsage = 0L;
    protected Long memoryPeak = 0L;
    protected Long memoryLimit = 0L;
    protected Double cpuUsage = 0.0;

    public CloudServerData(UUID serverUuid, int port, int maxPlayers) {
        this.serverUuid = serverUuid;
        this.port = port;
        this.maxPlayers = maxPlayers;
    }

    public void setPerformanceStats(Double tps, Double avgTps, Long memoryUsage, Long memoryPeak, Long memoryLimit, Double cpuUsage) {
        this.tps = tps;
        this.avgTps = avgTps;
        this.memoryUsage = memoryUsage;
        this.memoryPeak = memoryPeak;
        this.memoryLimit = memoryLimit;
        this.cpuUsage = cpuUsage;
    }
}