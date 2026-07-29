package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.setup.def.ConfigSetup;

@CommandDescription(name = "configure", description = "Reconfigure the cloud", aliases = {"reconfigure", "reconf", "conf"})
public final class ConfigureCommand extends Command {

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        new ConfigSetup().startSetup();
        return true;
    }
}