package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;

@CommandDescription(name = "reload", description = "Reload the cloud", aliases = {"rl"})
public final class ReloadCommand extends Command {

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        PocketCloud.instance().reload();
        return true;
    }
}