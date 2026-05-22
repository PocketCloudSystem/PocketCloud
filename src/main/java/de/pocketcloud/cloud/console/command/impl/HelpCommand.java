package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.LiteralCommandParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CommandDescription(name = "help", description = "List all commands", aliases = {"?"})
public final class HelpCommand extends Command {

    @Override
    public void prepare() {
        addParameter(new LiteralCommandParameter("command", true, "Command not found"));
    }

    @Override
    public boolean run(CommandSender sender, String label, Map<String, Object> args, Map<String, Object> flags) {
        List<Command> commands = new ArrayList<>();
        if (args.containsKey("command")) {
            commands.add((Command) args.get("command"));
        } else {
            commands.addAll(PocketCloud.getInstance().commandManager().getAll());
        }

        sender.info("Commands §8(§b{}§8)§r:", commands.size());
        for (Command command : commands) {
            if (command.getAliases().length > 0) {
                sender.info("§b{} §8- §r{} §8- §r[§c{}§r]", command.getName(), command.getDescription(), String.join("§8, §c", command.getAliases()));
            } else {
                sender.info("§b{} §8- §r{} §8- §r[]", command.getName(), command.getDescription());
            }
        }

        return true;
    }
}