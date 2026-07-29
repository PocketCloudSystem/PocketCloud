package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.sender.CommandSender;

@CommandDescription(name = "exit", description = "Shuts down the cloud")
public final class ExitCommand extends Command {

    @Override
    public void prepare() {
        addFlag(CommandFlag.shortFlag("y", true));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        if (ctx.hasFlag("y")) {
            PocketCloud.instance().shutdown();
        } else {
            awaitConfirmation(sender, "Are you sure you want to §cshutdown §rthe cloud?")
                    .thenSuccess(r -> {
                        if (r) {
                            PocketCloud.instance().shutdown();
                        }
                    });
        }
        return true;
    }
}