package de.pocketcloud.cloud.console.command;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.common.util.ArrayUtils;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public final class CommandManager implements Loadable, Tickable {

    private static final String[] acceptKeywords = new String[]{"y", "yes", "true"};

    private final Map<String, Command> commandPool = new HashMap<>();
    private final Map<String, Command> knownAliasesPool = new HashMap<>();
    private final List<AwaitConfirmationData> awaitConfirmationQueue = new ArrayList<>();
    private AwaitConfirmationData currentAwaitConfirmation = null;
    private long currentAwaitConfirmationExpiration = 0;

    @Override
    public void load() {
        Reflections reflections = new Reflections("de.pocketcloud.cloud.console.command.impl");
        Set<Class<? extends Command>> commandClasses = reflections.getSubTypesOf(Command.class);
        for (Class<? extends Command> commandClass : commandClasses) {
            try {
                register(commandClass.getDeclaredConstructor().newInstance());
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void unload() {
        commandPool.clear();
        knownAliasesPool.clear();
    }

    public Promise<Boolean> awaitConfirmation(Command command, CommandSender sender, String prompt, int timeoutInSeconds) {
        Promise<Boolean> promise = new Promise<>();
        awaitConfirmationQueue.add(new AwaitConfirmationData(command.getName(), sender, prompt, Arrays.stream(acceptKeywords).map(String::toLowerCase).toList().toArray(new String[0]), timeoutInSeconds * 20, promise));
        return promise;
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
        if (currentAwaitConfirmation != null) {
            Promise<Boolean> promise = currentAwaitConfirmation.promise;
            String[] acceptKeywords = currentAwaitConfirmation.acceptKeywords;
            if (Arrays.asList(acceptKeywords).contains(line.toLowerCase())) {
                promise.resolve(true);
            } else {
                sender.warn("§cCancelled the confirmation.");
                promise.resolve(false);
            }

            currentAwaitConfirmation = null;
            PocketCloud.instance().console().resetPrompt();
            return;
        }

        if (line.isBlank()) return;
        List<String> args = ArrayUtils.parseQuoteAware(line);
        String name = args.removeFirst();

        Command command;
        if ((command = get(name).orElse(null)) != null) {
            command.handle(sender, name, args);
        } else sender.error("§cCommand not found. §rRun §8'§bhelp§8' §rto receive a list of available commands.");
    }

    @Override
    public void tick(long currentTick) {
        if (currentAwaitConfirmation != null) {
            CommandSender sender = currentAwaitConfirmation.sender;
            long expiration = currentAwaitConfirmationExpiration;
            Promise<Boolean> promise = currentAwaitConfirmation.promise;
            if (expiration <= currentTick) {
                promise.reject(new TimeoutException("Confirmation timed out"));
                sender.warn("§cConfirmation timed out.");
                PocketCloud.instance().console().resetPrompt();
                currentAwaitConfirmation = null;
            }

            return;
        }

        if (!awaitConfirmationQueue.isEmpty()) {
            currentAwaitConfirmation = awaitConfirmationQueue.removeFirst();
            currentAwaitConfirmationExpiration = currentTick + currentAwaitConfirmation.timeout;
            String prompt = currentAwaitConfirmation.prompt.trim() + " §8[§aY§8/§cn§8] ";
            PocketCloud.instance().console().setPrompt(prompt);
        }
    }

    public Optional<Command> get(String name) {
        return Optional.ofNullable(commandPool.getOrDefault(name, knownAliasesPool.get(name)));
    }

    public List<Command> getAll() {
        return commandPool.values().stream().toList();
    }

    private record AwaitConfirmationData(String command, CommandSender sender, String prompt, String[] acceptKeywords, int timeout, Promise<Boolean> promise) {}
}