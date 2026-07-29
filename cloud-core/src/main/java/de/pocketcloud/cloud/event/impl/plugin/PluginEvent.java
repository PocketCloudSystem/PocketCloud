package de.pocketcloud.cloud.event.impl.plugin;

import de.pocketcloud.cloud.event.CloudEvent;
import de.pocketcloud.cloud.plugin.CloudPlugin;
import lombok.Getter;

public abstract class PluginEvent extends CloudEvent {

    @Getter
    private final CloudPlugin plugin;

    public PluginEvent(CloudPlugin plugin) {
        this.plugin = plugin;
    }
}