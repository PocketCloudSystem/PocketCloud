package de.pocketcloud.cloud.template;

import de.pocketcloud.cloud.config.impl.ServerSettingsConfig;
import de.pocketcloud.cloud.server.software.ServerSoftware;
import de.pocketcloud.cloud.util.FilterableObject;
import de.pocketcloud.cloud.util.PocketCloudPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public enum TemplateType implements FilterableObject {

    SERVER,
    PROXY;

    public Path globalTemplatePath() {
        return PocketCloudPaths.templates().global().with(name().toLowerCase()).asPath();
    }

    public ServerSettingsConfig.ServerPortRange serverPortRange() {
        return ServerSettingsConfig.instance().getServerPortRange(this);
    }

    public int timeout() {
        return ServerSettingsConfig.instance().getServerTimeout(this);
    }

    private final List<ServerSoftware> softwareList = new ArrayList<>();

    public void add(ServerSoftware software) {
        softwareList.add(software);
    }

    public void remove(ServerSoftware software) {
        softwareList.remove(software);
    }

    public boolean isProxy() {
        return this == PROXY;
    }

    public boolean isServer() {
        return this == SERVER;
    }

    public ServerSoftware get(String name) {
        return softwareList.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
    }

    public List<ServerSoftware> softwareList() {
        return softwareList;
    }
}