package de.pocketcloud.cloud.console.command;

import de.pocketcloud.cloud.console.command.impl.*;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.util.ArrayUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CommandManager implements Loadable {

    private final Map<String, Command> commandPool = new HashMap<>();
    private final Map<String, Command> knownAliasesPool = new HashMap<>();

    @Override
    public void load() {
        registerAll(new ExitCommand(), new HelpCommand(), new ReloadCommand(), new StatusCommand(), new DebugCommand());
    }

    @Override
    public void unload() {
        commandPool.clear();
        knownAliasesPool.clear();
    }

    public void register(Command command) {
        commandPool.put(command.getName(), command);
        for (String alias : command.getAliases()) knownAliasesPool.put(alias, command);
        for (SubCommand subCommand : command.getSubCommands().values()) subCommand.setParent(command);
    }

    public void registerAll(Command... commands) {
        for (Command command : commands) register(command);
    }

    public void call(CommandSender sender, String line) {
        List<String> args = ArrayUtils.parseQuoteAware(line);
        String name = args.removeFirst();

        Command command;
        if ((command = get(name).orElse(null)) != null) {
            command.handle(sender, name, args);
        } else sender.error("§cCommand not found. §rRun §8'§bhelp§8' §rto receive a list of available commands.");
    }

    public Optional<Command> get(String name) {
        return Optional.ofNullable(commandPool.getOrDefault(name, knownAliasesPool.get(name)));
    }

    public List<Command> getAll() {
        return commandPool.values().stream().toList();
    }
}