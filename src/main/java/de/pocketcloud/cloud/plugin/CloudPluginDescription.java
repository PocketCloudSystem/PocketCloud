package de.pocketcloud.cloud.plugin;

import lombok.Getter;

import java.util.List;

@Getter
public final class CloudPluginDescription {

    private String name;
    private String version;
    private String main;
    private List<String> authors;
    private String description;
}