package de.pocketcloud.cloud.template;

import de.pocketcloud.cloud.server.software.ServerSoftware;

import java.util.ArrayList;
import java.util.List;

public enum TemplateType {

    SERVER,
    PROXY;

    private final List<ServerSoftware> softwareList = new ArrayList<>();

    public void add(ServerSoftware software) {
        softwareList.add(software);
    }

    public void remove(ServerSoftware software) {
        softwareList.remove(software);
    }

    public ServerSoftware get(String name) {
        return softwareList.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
    }

    public List<ServerSoftware> softwareList() {
        return softwareList;
    }
}