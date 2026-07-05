package de.pocketcloud.cloud.server.config.impl;

import de.pocketcloud.cloud.server.config.ServerProperties;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.config.impl.MainConfig;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.util.ArrayUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WaterdogConfig extends ServerProperties {

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
                Map.entry("%uuid%", server.uuid().toString()),
                Map.entry("%name%", server.name()),
                Map.entry("%server_port%", server.serverData().port()),
                Map.entry("%max_players%", server.template().settings().getMaxPlayerCount()),
                Map.entry("%template%", server.templateName()),
                Map.entry("%address%", MainConfig.instance().getNetworkAddress().getHostString()),
                Map.entry("%port%", MainConfig.instance().getNetworkAddress().getPort()),
                Map.entry("%encryption%", MainConfig.instance().isNetworkEncryptionEnabled()),
                Map.entry("%language%", MainConfig.instance().getLanguage()),
                Map.entry("%cloud_path%", System.getProperty("user.dir")),
                Map.entry("%timeout%", server.template().templateType().timeout()),
                Map.entry("%auth_key%", PocketCloud.instance().network().authToken()),
                Map.entry("%packet_size_limit%", MainConfig.instance().getNetworkPacketSizeLimit())
        ));
    }

    @Override
    public Map<String, Object> getDefaultContent() {
        return ArrayUtils.orderedMap(
                "listener", ArrayUtils.orderedMap(
                        "motd", "%name%",
                        "name", "§bWaterdog§3PE",
                        "priorities", List.of(),
                        "host", "0.0.0.0:%server_port%",
                        "max_players", "%max_players%",
                        "forced_hosts", new LinkedHashMap<>(),
                        "additional_ports", List.of(),
                        "join_handler", "DefaultJoinHandler",
                        "reconnect_handler", "DefaultReconnectHandler"
                ),
                "servers", new LinkedHashMap<>(),
                "network_settings", ArrayUtils.orderedMap(
                        "connection_throttle", 10,
                        "connection_throttle_time", 1000,
                        "enable_ipv6", false,
                        "max_user_mtu", 1400,
                        "enable_cookies", true,
                        "login_throttle", 2,
                        "max_downstream_mtu", 1400,
                        "connection_timeout", 15
                ),
                "permissions", new LinkedHashMap<>(),
                "permissions_default", List.of(),
                "enable_debug", false,
                "upstream_encryption", true,
                "online_mode", true,
                "use_login_extras", false,
                "use_certificate_payload", true,
                "replace_username_spaces", false,
                "enable_query", true,
                "prefer_fast_transfer", true,
                "inject_proxy_commands", true,
                "compression", "zlib",
                "upstream_compression_level", 6,
                "downstream_compression_level", 2,
                "enable_edu_features", true,
                "enable_packs", true,
                "overwrite_client_packs", false,
                "force_server_packs", false,
                "pack_cache_size", 16,
                "default_idle_threads", -1,
                "enable_statistics", true,
                "enable_error_reporting", true,
                "server-uuid", "%uuid%",
                "cloud-address", "%address%",
                "cloud-port", "%port%",
                "server-name", "%name%",
                "template", "%template%",
                "network-encryption", "%encryption%",
                "cloud-language", "%language%",
                "cloud-path", "%cloud_path%",
                "server-timeout", "%timeout%",
                "auth-key", "%auth_key%",
                "packet-size-limit", "%packet_size_limit%"
        );
    }

    @Override
    public String getFileName() {
        return "config.yml";
    }

    @Override
    public ServerSoftware getServerSoftware() {
        return ServerSoftwareManager.instance().get("waterdogpe-latest");
    }
}