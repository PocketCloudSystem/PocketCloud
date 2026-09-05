package de.pocketcloud.cloud.http.route.v1;

import com.google.gson.JsonObject;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.annotation.ApiVersion;
import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.util.VersionInfo;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.Duration;

@ApiVersion(1)
public final class GeneralRoutes {

    @GetRoute("/stats")
    public void stats(HttpRequest request, HttpResponse response) {
        String currentVersion = VersionInfo.VERSION;
        boolean beta = VersionInfo.BETA;
        int serverCount = PocketCloud.instance().servers().serverCount();
        int templateCount = PocketCloud.instance().templates().templateCount();
        int playerCount = PocketCloud.instance().players().playerCount();
        int groupCount = PocketCloud.instance().serverGroups().groupsCount();
        int pluginCount = PocketCloud.instance().plugins().pluginCount();
        Duration uptime = PocketCloud.instance().uptime();
        long totalAvgTrafficIn = PocketCloud.instance().traffic().averageBytesAll(TrafficDirection.IN);
        long totalAvgTrafficOut = PocketCloud.instance().traffic().averageBytesAll(TrafficDirection.OUT);
        long totalTrafficIn = PocketCloud.instance().traffic().totalBytesAll(TrafficDirection.IN);
        long totalTrafficOut = PocketCloud.instance().traffic().totalBytesAll(TrafficDirection.OUT);
        double tps = PocketCloud.instance().performanceStats().currentTPS();
        double avgTps = PocketCloud.instance().performanceStats().averageTPS();
        double tickUsage = PocketCloud.instance().performanceStats().tickUsage();
        double usedMemory = PocketCloud.instance().performanceStats().processUsedMemory();
        double maxMemory = PocketCloud.instance().performanceStats().processMaxMemory();
        double cpuUsage = PocketCloud.instance().performanceStats().processCpuUsage();

        response.status(HttpResponseStatus.OK)
                .json(obj -> {
                    obj.addProperty("version", currentVersion);
                    obj.addProperty("beta", beta);
                    obj.addProperty("server_count", serverCount);
                    obj.addProperty("player_count", playerCount);
                    obj.addProperty("template_count", templateCount);
                    obj.addProperty("server_group_count", groupCount);
                    obj.addProperty("plugin_count", pluginCount);
                    obj.addProperty("uptime_ms", uptime.toMillis());

                    JsonObject totalAvgTraffic = new JsonObject();
                    totalAvgTraffic.addProperty("in", totalAvgTrafficIn);
                    totalAvgTraffic.addProperty("out", totalAvgTrafficOut);
                    obj.add("total_avg_traffic", totalAvgTraffic);

                    JsonObject totalTraffic = new JsonObject();
                    totalTraffic.addProperty("in", totalTrafficIn);
                    totalTraffic.addProperty("out", totalTrafficOut);
                    obj.add("total_traffic", totalTraffic);

                    obj.addProperty("tps", tps);
                    obj.addProperty("avg_tps", avgTps);
                    obj.addProperty("tick_usage", tickUsage);
                    obj.addProperty("used_memory", usedMemory);
                    obj.addProperty("max_memory", maxMemory);
                    obj.addProperty("cpu_usage", cpuUsage);
                });
    }
}