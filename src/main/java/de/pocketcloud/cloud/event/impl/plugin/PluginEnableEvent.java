package de.pocketcloud.cloud.event.impl.plugin;

import de.pocketcloud.cloud.plugin.CloudPlugin;

public final class PluginEnableEvent extends PluginEvent {

    public PluginEnableEvent(CloudPlugin plugin) {
        super(plugin);
    }
}