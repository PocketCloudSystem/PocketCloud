package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.StringEnumParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.sub.SubCommand;

import java.util.List;
import java.util.Map;

@CommandDescription(name = "exit", description = "Shuts down the cloud")
public final class ExitCommand extends Command {

    @Override
    public void prepare() {
        addFlag(CommandFlag.shortFlag("y", true));
        addParameter(new StringEnumParameter("hi", List.of("moin"), false, true));
        registerSubCommand(SubCommand.lambda("test", (commandSender, s, stringObjectMap, stringObjectMap2) -> {
            commandSender.success("Whats up g");
            return true;
        }, false));
    }

    @Override
    public boolean run(CommandSender sender, String label, Map<String, Object> args, Map<String, Object> flags) {
        if (flags.containsKey("y")) {
            PocketCloud.getInstance().shutdown();
        }

        return true;
    }
}