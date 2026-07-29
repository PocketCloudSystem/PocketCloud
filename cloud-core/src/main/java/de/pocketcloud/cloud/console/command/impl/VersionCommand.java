package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.util.VersionInfo;

@CommandDescription(name = "version", description = "View the cloud's version", aliases = {"ver"})
public final class VersionCommand extends Command {

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        sender.info("Your version of §bPocket§3Cloud §ris currently running on §b{}§r.", VersionInfo.VERSION + (VersionInfo.BETA ? "§r§c@BETA" : ":"));
        sender.info("Contributors: §b{}", String.join("§8, §b", VersionInfo.DEVELOPERS));
        return true;
    }
}