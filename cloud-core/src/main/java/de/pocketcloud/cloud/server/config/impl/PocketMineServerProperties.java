package de.pocketcloud.cloud.server.config.impl;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.config.ServerProperties;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.util.ArrayUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PocketMineServerProperties extends ServerProperties {

    @Override
    public boolean modify(String filePath, Map<String, Object> updatedContent) {
        return modifyProperties(filePath, updatedContent);
    }

    @Override
    public boolean renew(String filePath) {
        return renewProperties(filePath);
    }

    @Override
    public boolean needsRenewal(String filePath) {
        return needsRenewalProperties(filePath);
    }

    @Override
    public Map<String, Object> replacePlaceholders(CloudServer server) {
        return new LinkedHashMap<>(Map.ofEntries(
            Map.entry("%uuid%", server.uuid().toString()),
            Map.entry("%name%", server.name()),
            Map.entry("%server_port%", server.data().port()),
            Map.entry("%server_portv6%", server.data().port() + 1),
            Map.entry("%max_players%", server.template().settings().maxPlayerCount()),
            Map.entry("%template%", server.templateName()),
            Map.entry("%address%", PocketCloud.instance().config().network().address()),
            Map.entry("%port%", PocketCloud.instance().config().network().port()),
            Map.entry("%encryption%", PocketCloud.instance().config().network().encryption()),
            Map.entry("%language%", PocketCloud.instance().config().language()),
            Map.entry("%cloud_path%", System.getProperty("user.dir")),
            Map.entry("%timeout%", TemplateTypeHelper.timeout(server.template().templateType())),
            Map.entry("%auth_key%", PocketCloud.instance().network().authToken()),
            Map.entry("%server_ip%", !PocketCloud.instance().clients().getAll(c -> {
                if (c.hasServer()) {
                    return c.server().template().templateType().isProxy();
                }

                return false;
            }).isEmpty() ? "127.0.0.1" : "0.0.0.0"),
            Map.entry("%packet_size_limit%", PocketCloud.instance().config().network().packetSizeLimit())
        ));
    }

    @Override
    public Map<String, Object> getDefaultContent() {
        return ArrayUtils.orderedMap(
                "language", "eng",
                "motd", "§b%name%",
                "server-port", "%server_port%",
                "server-portv6", "%server_portv6%",
                "server-ip", "%server_ip%",
                "server-ipv6", "::1",
                "enable-ipv6", "on",
                "white-list", "off",
                "max-players", "%max_players%",
                "gamemode", "SURVIVAL",
                "force-gamemode", "off",
                "hardcore", "off",
                "pvp", "on",
                "difficulty", 2,
                "generator-settings", "",
                "level-name", "world",
                "level-seed", "",
                "level-type", "DEFAULT",
                "enable-query", "on",
                "auto-save", "off",
                "view-distance", 16,
                "xbox-auth", "off",
                "server-uuid", "%uuid%",
                "server-name", "%name%",
                "template", "%template%",
                "cloud-address", "%address%",
                "cloud-port", "%port%",
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
        return "server.properties";
    }

    @Override
    public IServerSoftware getServerSoftware() {
        return PocketCloud.instance().softwareList().get("pmmp-latest");
    }
}