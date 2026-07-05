package de.pocketcloud.cloud.event.impl.plugin;

import de.pocketcloud.cloud.plugin.CloudPlugin;

public class PluginLoadEvent extends PluginEvent {

    public PluginLoadEvent(CloudPlugin plugin) {
        super(plugin);
    }
}