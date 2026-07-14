package de.pocketcloud.cloud.config;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogLevel;
import de.pocketcloud.cloud.server.start.ServerStartMethod;
import de.pocketcloud.cloud.server.util.ServerStartMethods;
import de.pocketcloud.configlib.Comment;
import de.pocketcloud.configlib.ConfigMap;
import de.pocketcloud.configlib.ConfigType;
import de.pocketcloud.configlib.Configuration;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public final class ServerSettingsConfig extends Configuration {

    @Comment({"Server start method, used to boot a server.", "Available: proc, screen, tmux"})
    private String startMethod = "proc";
    @Comment({"Server timeouts for the template types."})
    private ConfigMap serverTimeouts = new ConfigMap()
            .set(TemplateType.SERVER.name().toLowerCase(), 15, "Timeout for SERVER-type servers in seconds")
            .set(TemplateType.PROXY.name().toLowerCase(), 15, "Timeout for PROXY-type servers in seconds");
    @Comment({"Server port ranges for the template types."})
    private ConfigMap serverPortRanges = new ConfigMap()
            .set(TemplateType.SERVER.name().toLowerCase(), new ConfigMap().set("start", 40000)
                    .set("end", 65535)
                    .set("random-ports", true, "Whether the port should be randomly generated between start and end"),
                    "Server port ranges")
            .set(TemplateType.PROXY.name().toLowerCase(), new ConfigMap().set("start", 19132)
                            .set("end", 20000)
                            .set("random-ports", false, "Whether the port should be randomly generated between start and end"),
                    "Server port ranges");

    public ServerSettingsConfig() {
        super("storage/configs/server_settings.yml", ConfigType.YAML);
        reload();
    }

    public void reload() {
        var changes = new AtomicInteger(0);
        boolean loadFailed = !load(changes);
        
        LinkedHashMap<String, Object> defaultPortRanges = serverPortRanges.toRawMap();

        if (ServerStartMethods.get(startMethod.toLowerCase()).isEmpty()) {
            startMethod = "proc";
            changes.incrementAndGet();
        }

        ServerStartMethod current = ServerStartMethods.get(startMethod.toLowerCase()).orElse(null);
        if (current != null) {
            if (!current.isAvailable()) {
                PocketCloud.instance().appendStartNotification("Invalid server start method §8'§b{}§8'§r: §cMethod is not available on this machine.", CloudLogLevel.WARN, startMethod);
                startMethod = "proc";
                changes.incrementAndGet();
            }
        }

        for (Map.Entry<String, Object> entry : serverPortRanges.toRawMap().entrySet()) {
            @SuppressWarnings("unchecked")
            LinkedHashMap<String, Object> portData = (LinkedHashMap<String, Object>) entry.getValue();

            int start = Integer.parseInt(portData.get("start").toString());
            int end = Integer.parseInt(portData.get("end").toString());

            if (start <= 0 || end <= 0) {
                changes.incrementAndGet();
                PocketCloud.instance().appendStartNotification("Invalid port range §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r: §bStart §7or §bend §7can not be less or equal to §b0§r: §cResetting the entry, please review your config...", CloudLogLevel.WARN, start, end, entry.getKey());
                serverPortRanges.set(entry.getKey(), defaultPortRanges.get(entry.getKey()));
            } else if (start > end) {
                changes.incrementAndGet();
                PocketCloud.instance().appendStartNotification("Invalid port range §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r: §bStart §ris §chigher §rthan §bend§r: §cResetting the entry, please review your config...", CloudLogLevel.WARN, start, end, entry.getKey());
                serverPortRanges.set(entry.getKey(), defaultPortRanges.get(entry.getKey()));
            } else if ((start + 50) > end) {
                changes.incrementAndGet();
                PocketCloud.instance().appendStartNotification("Invalid port range §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r: §bEnd §rneeds to be at least §b50 ports higher §rthan §bstart§r: §cResetting the entry, please review your config...", CloudLogLevel.WARN, start, end, entry.getKey());
                serverPortRanges.set(entry.getKey(), defaultPortRanges.get(entry.getKey()));
            } else {
                PocketCloud.instance().appendStartNotification("Loaded server port range configuration §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r.", CloudLogLevel.SUCCESS, start, end, entry.getKey());
            }
        }

        ServerStartMethods.set(ServerStartMethods.get(startMethod).orElse(ServerStartMethods.get("proc").get()));

        if (loadFailed || changes.get() > 0) save();
    }

    public void setServerPortRange(TemplateType type, int start, int end, boolean random) {
        serverPortRanges.set(type.name().toLowerCase(), new ConfigMap().set("start", start).set("end", end).set("random-ports", random));
    }

    public void setServerTimeout(TemplateType type, int timeout) {
        if (timeout <= 0) return;
        serverTimeouts.set(type.name().toLowerCase(), timeout);
    }

    @SuppressWarnings("unchecked")
    public ServerPortRange getServerPortRange(TemplateType type) {
        LinkedHashMap<String, Object> defaultPortRanges = (LinkedHashMap<String, Object>) serverPortRanges.toRawMap().getOrDefault(type.name().toLowerCase(), new LinkedHashMap<String, Object>());
        return new ServerPortRange(
                type,
                Integer.parseInt(defaultPortRanges.getOrDefault("start", 20000).toString()),
                Integer.parseInt(defaultPortRanges.getOrDefault("end", 65535).toString()),
                Boolean.parseBoolean(defaultPortRanges.getOrDefault("random-ports", false).toString())
        );
    }

    public int getServerTimeout(TemplateType type) {
        return (int) Optional.ofNullable(serverTimeouts.get(type.name().toLowerCase())).orElse(20);
    }

    public record ServerPortRange(TemplateType type, int start, int end, boolean random) {

        public boolean inRange(int port) {
            return port >= start && port <= end;
        }
    }
}