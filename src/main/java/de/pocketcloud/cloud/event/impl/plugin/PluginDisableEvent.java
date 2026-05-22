package de.pocketcloud.cloud.event.impl.plugin;

import de.pocketcloud.cloud.plugin.CloudPlugin;

public final class PluginDisableEvent extends PluginEvent {

    public PluginDisableEvent(CloudPlugin plugin) {
        super(plugin);
    }
}