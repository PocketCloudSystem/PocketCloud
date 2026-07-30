package de.pocketcloud.cloud.console.command.impl.plugin;

import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;

@CommandDescription(name = "plugin", description = "Manage the plugins", aliases = {"pl"})
public final class PluginCommand extends Command {

    @Override
    public void prepare() {
        //todo
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }
}