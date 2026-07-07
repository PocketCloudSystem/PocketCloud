package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.config.LogSettingsConfig;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.log.CloudLogger;

@CommandDescription(name = "debug", description = "Toggle the debug mode")
public final class DebugCommand extends Command {

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        LogSettingsConfig.instance().setDebugMode(!LogSettingsConfig.instance().isDebugMode());
        LogSettingsConfig.instance().save();
        CloudLogger.get().setDebugMode(LogSettingsConfig.instance().isDebugMode());

        if (LogSettingsConfig.instance().isDebugMode()) {
            sender.success("Successfully §aenabled §rthe §6debug mode§r.");
        } else {
            sender.success("Successfully §cdisabled §rthe §6debug mode§r.");
        }

        return true;
    }
}