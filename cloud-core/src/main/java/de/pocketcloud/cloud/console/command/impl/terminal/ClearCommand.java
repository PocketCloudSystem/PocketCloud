package de.pocketcloud.cloud.console.command.impl.terminal;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;

@CommandDescription(name = "clear", description = "Clears the console", aliases = {"cls"})
public final class ClearCommand extends Command {

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        PocketCloud.instance().console().clear();
        return true;
    }
}