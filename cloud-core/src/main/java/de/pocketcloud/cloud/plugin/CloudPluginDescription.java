package de.pocketcloud.cloud.plugin;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
public final class CloudPluginDescription {

    private String name;
    private String version;
    private String main;
    private List<String> authors;
    private String description;
}