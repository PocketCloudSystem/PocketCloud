package de.pocketcloud.cloud.plugin;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Accessors(fluent = true)
public final class CloudPluginDescription {

    private String name;
    private String version;
    private String main;
    private List<String> authors = new ArrayList<>();
    private String description = "";

    @SuppressWarnings("unchecked")
    public static CloudPluginDescription read(Map<String, Object> raw) {
        CloudPluginDescription description = new CloudPluginDescription();
        description.name = (String) raw.get("name");
        description.version = String.valueOf(raw.get("version"));
        description.main = (String) raw.get("main");
        if (raw.containsKey("authors")) {
            Object authors = raw.get("authors");
            if (authors instanceof List) {
                description.authors = (List<String>) raw.get("authors");
            }
        }

        if (raw.containsKey("author")) {
            description.authors.add(String.valueOf(raw.get("author")));
        }

        description.description = (String) raw.getOrDefault("description", "");
        return description;
    }
}