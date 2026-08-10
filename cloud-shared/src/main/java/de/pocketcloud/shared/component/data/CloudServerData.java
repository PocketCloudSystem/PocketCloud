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
    protected final String address;
    protected final int port;
    protected int maxPlayers;

    protected Long processId = null;
    protected Double tps = null;
    protected Double avgTps = null;
    protected Long memoryUsage = null;
    protected Long memoryPeak = null;
    protected Long memoryLimit = null;
    protected Double cpuUsage = null;

    public CloudServerData(UUID serverUuid, String address, int port, int maxPlayers) {
        this.serverUuid = serverUuid;
        this.address = address;
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