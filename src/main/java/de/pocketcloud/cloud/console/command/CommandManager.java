package de.pocketcloud.cloud.console.command;

import de.pocketcloud.cloud.console.command.impl.ExitCommand;
import de.pocketcloud.cloud.console.command.impl.HelpCommand;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;

import java.util.*;

public final class CommandManager {

    private final Map<String, Command> commandPool = new HashMap<>();
    private final Map<String, Command> knownAliasesPool = new HashMap<>();

    public CommandManager() {
        register(new ExitCommand());
        register(new HelpCommand());
    }

    public void register(Command command) {
        commandPool.put(command.getName(), command);
        for (String alias : command.getAliases()) knownAliasesPool.put(alias, command);
        for (SubCommand subCommand : command.getSubCommands().values()) subCommand.setParent(command);
    }

    public void registerAll(Command... commands) {
        for (Command command : commands) register(command);
    }

    public void call(CommandSender sender, String[] args) {
        var arguments = new ArrayList<>(Arrays.stream(args).toList());
        var name = arguments.removeFirst();

        Command command;
        if ((command = get(name).orElse(null)) != null) {
            command.handle(sender, name, arguments);
        }
    }

    public Optional<Command> get(String name) {
        return Optional.ofNullable(commandPool.getOrDefault(name, knownAliasesPool.get(name)));
    }

    public List<Command> getAll() {
        return commandPool.values().stream().toList();
    }
}