package de.pocketcloud.cloud.server.config.impl;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.config.ServerProperties;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.util.ArrayUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PocketMineConfig extends ServerProperties {

    @Override
    public boolean modify(String filePath, Map<String, Object> updatedContent) {
        return modifyYaml(filePath, updatedContent);
    }

    @Override
    public boolean renew(String filePath) {
        return renewYaml(filePath);
    }

    @Override
    public boolean needsRenewal(String filePath) {
        return needsRenewalYaml(filePath);
    }

    @Override
    public Map<String, Object> replacePlaceholders(CloudServer server) {
        return Map.of();
    }

    @Override
    public Map<String, Object> getDefaultContent() {
        return ArrayUtils.orderedMap(
                "settings", ArrayUtils.orderedMap(
                        "force-language", false,
                        "shutdown-message", "Server closed",
                        "query-plugins", true,
                        "enable-profiling", false,
                        "profile-report-trigger", 20,
                        "async-workers", "auto",
                        "enable-dev-builds", false
                ),
                "memory", ArrayUtils.orderedMap(
                        "global-limit", 0,
                        "main-limit", 0,
                        "main-hard-limit", 1024,
                        "async-worker-hard-limit", 256,
                        "check-rate", 20,
                        "continuous-trigger", true,
                        "continuous-trigger-rate", 30,
                        "garbage-collection", ArrayUtils.orderedMap("period", 36000),
                        "memory-dump", ArrayUtils.orderedMap("dump-async-worker", true),
                        "max-chunks", ArrayUtils.orderedMap("chunk-radius", 4)
                ),
                "network", ArrayUtils.orderedMap(
                        "batch-threshold", 256,
                        "compression-level", 6,
                        "async-compression", false,
                        "async-compression-threshold", 10000,
                        "upnp-forwarding", false,
                        "max-mtu-size", 1492,
                        "enable-encryption", true
                ),
                "debug", ArrayUtils.orderedMap("level", 1),
                "player", ArrayUtils.orderedMap("save-player-data", true, "verify-xuid", true),
                "level-settings", ArrayUtils.orderedMap("default-format", "leveldb"),
                "chunk-sending", ArrayUtils.orderedMap("per-tick", 4, "spawn-radius", 4),
                "chunk-ticking", ArrayUtils.orderedMap(
                        "tick-radius", 3,
                        "blocks-per-subchunk-per-tick", 3,
                        "disable-block-ticking", List.of()
                ),
                "chunk-generation", ArrayUtils.orderedMap("population-queue-size", 32),
                "ticks-per", ArrayUtils.orderedMap("autosave", 6000),
                "auto-report", ArrayUtils.orderedMap(
                        "enabled", true,
                        "send-code", true,
                        "send-settings", true,
                        "send-phpinfo", false,
                        "use-https", true,
                        "host", "crash.pmmp.io"
                ),
                "anonymous-statistics", ArrayUtils.orderedMap("enabled", false, "host", "stats.pocketmine.net"),
                "auto-updater", ArrayUtils.orderedMap(
                        "enabled", true,
                        "on-update", ArrayUtils.orderedMap("warn-console", true),
                        "preferred-channel", "stable",
                        "suggest-channels", true,
                        "host", "update.pmmp.io"
                ),
                "timings", ArrayUtils.orderedMap("host", "timings.pmmp.io"),
                "console", ArrayUtils.orderedMap("enable-input", true, "title-tick", true),
                "aliases", new LinkedHashMap<>(),
                "worlds", new LinkedHashMap<>(),
                "plugins", ArrayUtils.orderedMap("legacy-data-dir", false)
        );
    }

    @Override
    public String getFileName() {
        return "pocketmine.yml";
    }

    @Override
    public ServerSoftware getServerSoftware() {
        return ServerSoftwareManager.instance().get("pmmp-latest");
    }
}