package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.StringEnumParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.sub.SubCommand;

import java.util.List;

@CommandDescription(name = "exit", description = "Shuts down the cloud")
public final class ExitCommand extends Command {

    @Override
    public void prepare() {
        addFlag(CommandFlag.shortFlag("y", true));
        addParameter(new StringEnumParameter("hi", List.of("moin"), false, true));
        registerSubCommand(SubCommand.lambda("test", (commandSender, _) -> {
            commandSender.success("Whats up g");
            return true;
        }, false));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        if (ctx.flags().containsKey("y")) {
            PocketCloud.instance().shutdown();
        }

        return true;
    }
}