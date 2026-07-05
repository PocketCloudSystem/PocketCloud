package de.pocketcloud.cloud.event.impl.plugin;

import de.pocketcloud.cloud.event.Event;
import de.pocketcloud.cloud.plugin.CloudPlugin;
import lombok.Getter;

public abstract class PluginEvent extends Event {

    @Getter
    private final CloudPlugin plugin;

    public PluginEvent(CloudPlugin plugin) {
        this.plugin = plugin;
    }
}