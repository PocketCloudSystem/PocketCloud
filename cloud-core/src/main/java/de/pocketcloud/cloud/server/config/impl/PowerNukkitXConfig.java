package de.pocketcloud.cloud.server.config.impl;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.config.ServerProperties;
import de.pocketcloud.common.util.ArrayUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PowerNukkitXConfig extends ServerProperties {

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
        return new LinkedHashMap<>(Map.ofEntries(
                Map.entry("%name%", server.name()),
                Map.entry("%server_port%", server.data().port()),
                Map.entry("%max_players%", server.template().settings().maxPlayerCount()),
                Map.entry("%auto_save%", server.template().settings().saveOnShutdown() || server.template().settings().staticServers()),
                Map.entry("%server_ip%", server.data().address())
        ));
    }

    @Override
    public Map<String, Object> getDefaultContent() {
        Map<String, Object> settings = ArrayUtils.orderedMap(
                "ip", "%server_ip%",
                "port", "%server_port%",
                "maxPlayers", "%max_players%",
                "defaultLevelName", "world",
                "allowList", false,
                "allowListMessage", "Server is white-listed",
                "motd", "%name%",
                "sub-motd", "powernukkitx.org",
                "language", "eng",
                "forceServerTranslate", false,
                "safeSpawn", true,
                "autoSave", "%auto_save%",
                "autosaveDelay", 6000,
                "saveUnknownBlock", true,
                "xboxAuth", false,
                "waterdogpe", false
        );

        Map<String, Object> playerSettings = ArrayUtils.orderedMap(
                "savePlayerData", true,
                "skinChangeCooldown", 30,
                "forceSkinTrusted", false,
                "checkMovement", true,
                "rotationUpdateThreshold", 1.0,
                "movementDistanceThreshold", 0.1,
                "spawnRadius", 16
        );

        Map<String, Object> gameplaySettings = ArrayUtils.orderedMap(
                "enableCommandBlocks", true,
                "allowBeta", false,
                "enableRedstone", true,
                "tickRedstone", true,
                "viewDistance", 8,
                "achievements", true,
                "announceAchievements", true,
                "spawnProtection", 16,
                "gamemode", 0,
                "forceGamemode", false,
                "hardcore", false,
                "pvp", true,
                "difficulty", 1,
                "allowNether", true,
                "allowTheEnd", true,
                "forceResources", false,
                "allowClientPacks", true,
                "serverAuthoritativeMovement", "server-auth",
                "allowVibrantVisuals", true,
                "experiments", new ArrayList<>(List.of(
                        "data_driven_biomes",
                        "experimental_creator_cameras",
                        "gametest",
                        "jigsaw_structures",
                        "upcoming_creator_features",
                        "villager_trades_rebalance",
                        "voxel_shapes"
                )),
                "cacheStructures", false,
                "enableEducation", false,
                "muteEmoteAnnouncements", false,
                "enableMobAi", true,
                "enableRecipes", true,
                "enableCreativeInventory", true,
                "enableDaylightCycle", true,
                "enableWeather", true,
                "enableEntitySpawning", true,
                "enableBlockRandomTicking", true,
                "enableLiquidFlow", true,
                "enableItemDrops", true,
                "enableXpOrbs", true,
                "enableExplosionBlockDamage", true,
                "enableBlockGravity", true,
                "enableHunger", true
        );

        Map<String, Object> miscSettings = ArrayUtils.orderedMap(
                "shutdownMessage", "Server closed",
                "installSpark", false,
                "bypassAPICheck", false,
                "overrideServerAuthBlockBreaking", false,
                "enableMetrics", true,
                "enableTerra", false,
                "disableMetrics", true
        );

        Map<String, Object> levelSettings = ArrayUtils.orderedMap(
                "levelThread", false,
                "autoTickRate", true,
                "autoTickRateLimit", 20,
                "baseTickRate", 1,
                "alwaysTickPlayers", false,
                "loadAllLevels", true,
                "chunkUnloadDelay", 15000,
                "entitySpawnCap", 512,
                "fieldOfView", 100,
                "levelWorkerThreads", -1
        );

        Map<String, Object> chunkSettings = ArrayUtils.orderedMap(
                "spawnLimit", 3,
                "perTickSend", 32,
                "spawnThreshold", 56,
                "chunksPerTicks", -1,
                "tickRadius", 4,
                "lightUpdates", true,
                "clearTickList", true,
                "generationQueueSize", 8,
                "saveGenerated", true,
                "convertBDSChunks", false,
                "disableBlockTicking", new ArrayList<>()
        );

        Map<String, Object> rateLimit = ArrayUtils.orderedMap(
                "rateLimitEnabled", true,
                "maxInboundPacketsPerSecond", 1500,
                "maxPacketsPerTick", 500,
                "maxCommandsPerSecondPerPlayer", 10,
                "maxChatPerSecondPerPlayer", 2,
                "maxFormResponsesPerSecondPerPlayer", 20,
                "maxMovementPacketsPerSecondPerPlayer", 40
        );

        Map<String, Object> botnet = ArrayUtils.orderedMap(
                "detectionEnabled", false,
                "suspiciousThreshold", 300,
                "minSuspiciousIps", 3,
                "autoBlock", true,
                "autoBlockDurationSeconds", 60,
                "minScore", 2
        );

        Map<String, Object> networkSettings = ArrayUtils.orderedMap(
                "queryPlugins", true,
                "compressionLevel", 4,
                "zlibProvider", 3,
                "snappy", false,
                "compressionBufferSize", 1048576,
                "maxDecompressSize", 268435456,
                "packetLimit", 8000,
                "enableQuery", true,
                "networkEncryption", true,
                "checkLoginTime", false,
                "autoFlush", true,
                "flushInterval", 10,
                "maxQueuedBytes", 67108864,
                "cookieMode", "ACTIVE",
                "rate-limit", rateLimit,
                "botnet", botnet
        );

        Map<String, Object> debugSettings = ArrayUtils.orderedMap(
                "deprecatedVerbose", true,
                "level", "INFO",
                "command", false,
                "mode", false,
                "packetList", new ArrayList<>(),
                "disableEncodingLimits", false
        );

        Map<String, Object> performanceSettings = ArrayUtils.orderedMap(
                "asyncWorkers", "auto",
                "baseTps", 20,
                "registryCacheEnabled", false,
                "registryCachePath", "path/to/your/registry_cache.bin",
                "forceGCpercentage", 1.0,
                "enable", true,
                "slots", 32,
                "defaultTemperature", 32,
                "freezingPoint", 0,
                "boilingPoint", 1024,
                "absoluteZero", -256,
                "melting", 16,
                "singleOperation", 1,
                "batchOperation", 32
        );

        Map<String, Object> config = ArrayUtils.orderedMap(
                "version", "3.0.0"
        );

        return ArrayUtils.orderedMap(
                "settings", settings,
                "player-settings", playerSettings,
                "gameplay-settings", gameplaySettings,
                "misc-settings", miscSettings,
                "level-settings", levelSettings,
                "chunk-settings", chunkSettings,
                "network-settings", networkSettings,
                "debug-settings", debugSettings,
                "performance-settings", performanceSettings,
                "config", config
        );
    }

    @Override
    public String getFileName() {
        return "pnx.yml";
    }

    @Override
    public IServerSoftware getServerSoftware() {
        return PocketCloud.instance().softwares().get("powernukkitx-latest").orElseThrow(() -> new RuntimeException("Required software not found"));
    }
}