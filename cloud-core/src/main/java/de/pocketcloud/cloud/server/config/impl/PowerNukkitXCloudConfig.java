package de.pocketcloud.cloud.server.config.impl;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.config.ServerProperties;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.util.ArrayUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public final class PowerNukkitXCloudConfig extends ServerProperties {

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
                Map.entry("%template%", server.templateName()),
                Map.entry("%address%", PocketCloud.instance().config().network().address()),
                Map.entry("%port%", PocketCloud.instance().config().network().port()),
                Map.entry("%language%", PocketCloud.instance().config().language()),
                Map.entry("%cloud_path%", System.getProperty("user.dir")),
                Map.entry("%auth_key%", PocketCloud.instance().network().authToken()),
                Map.entry("%encryption%", PocketCloud.instance().config().network().encryption()),
                Map.entry("%packet_size_limit%", PocketCloud.instance().config().network().packetSizeLimit()),
                Map.entry("%timeout%", TemplateTypeHelper.timeout(server.template().templateType()))
        ));
    }

    @Override
    public Map<String, Object> getDefaultContent() {
        return ArrayUtils.orderedMap(
                "server-name", "%name%",
                "template-name", "%template%",
                "server-uuid", "%uuid%",
                "cloud-language", "%language%",
                "server-timeout", "%timeout%",
                "cloud-path", "%cloud_path%",
                "network-address", "%address%",
                "network-port", "%port%",
                "network-auth-key", "%auth_key%",
                "network-encryption", "%packet_size_limit%",
                "network-packet-size-limit", "%port%"
        );
    }

    @Override
    public String getFileName() {
        return "pnx_cloud.yml";
    }

    @Override
    public IServerSoftware getServerSoftware() {
        return PocketCloud.instance().softwares().get("powernukkitx-latest").orElseThrow(() -> new RuntimeException("Required software not found"));
    }
}