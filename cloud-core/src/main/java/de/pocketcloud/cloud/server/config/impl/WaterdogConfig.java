package de.pocketcloud.cloud.server.config.impl;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.config.ServerProperties;

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
        return Map.ofEntries(
                Map.entry("%name%", server.name()),
                Map.entry("%server_ip%", server.data().address()),
                Map.entry("%server_port%", server.data().port()),
                Map.entry("%max_players%", server.template().settings().maxPlayerCount())
        );
    }

    @Override
    public String getFileName() {
        return "config.yml";
    }

    @Override
    public IServerSoftware getServerSoftware() {
        return PocketCloud.instance().softwares().get("waterdogpe-latest").orElseThrow(() -> new RuntimeException("Required software not found"));
    }
}