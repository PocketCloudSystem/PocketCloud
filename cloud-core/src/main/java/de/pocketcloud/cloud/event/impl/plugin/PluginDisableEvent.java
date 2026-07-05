package de.pocketcloud.cloud.event.impl.plugin;

import de.pocketcloud.cloud.plugin.CloudPlugin;

public class PluginDisableEvent extends PluginEvent {

    public PluginDisableEvent(CloudPlugin plugin) {
        super(plugin);
    }
}