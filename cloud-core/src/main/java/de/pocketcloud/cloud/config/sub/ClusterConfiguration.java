package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.cloud.console.log.CloudLogger;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Exclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public final class ClusterConfiguration extends OkaeriConfig implements ICloudConfig {

    @Exclude
    public static final List<String> STRATEGIES = List.of("least-memory", "fill", "round-robin");

    @Comment({"How the head picks the node for a new server (only used with more than one node).",
            "least-memory: the node with the most free memory budget (even distribution)",
            "fill: the node with the smallest free budget that is still enough (keeps machines free)",
            "round-robin: the next node in turn"})
    private String strategy = "least-memory";

    @Override
    public void validate() {
        if (strategy == null || !STRATEGIES.contains(strategy.toLowerCase())) {
            CloudLogger.get().warn("Invalid cluster strategy §b{}§r, reset to default... §8(§bleast-memory§8)", strategy);
            strategy = "least-memory";
        }
        strategy = strategy.toLowerCase();
    }
}
