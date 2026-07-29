package de.pocketcloud.cloud.console.command;

import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.exception.FlagParseException;
import de.pocketcloud.cloud.console.command.exception.NoArgumentFoundException;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.holder.CommandUtilityHolder;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.common.concurrent.Promise;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public abstract class Command extends CommandUtilityHolder {

    private final String name;
    private final String description;
    private final String usage;
    private final String[] aliases;

    private final Map<String, SubCommand> subCommands = new HashMap<>();
    
    public Command() {
        if (!this.getClass().isAnnotationPresent(CommandDescription.class)) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " requires @CommandDescription");
        }

        CommandDescription annotation = getClass().getAnnotation(CommandDescription.class);
        name = annotation.name();
        usage = annotation.usage().isBlank() ? null : annotation.usage();
        description = annotation.description();
        aliases = annotation.aliases();

        prepare();
    }

    public Promise<Boolean> awaitConfirmation(CommandSender sender, String prompt) {
        return awaitConfirmation(sender, prompt, 10);
    }

    public Promise<Boolean> awaitConfirmation(CommandSender sender, String prompt, int timeoutInSeconds) {
        return PocketCloud.instance().commands().awaitConfirmation(this, sender, prompt, timeoutInSeconds);
    }

    abstract public void prepare();

    public void handle(CommandSender sender, String label, List<String> args) {
        ScanResult commandFlags;
        try {
            commandFlags = scanAndCleanFlags(args);
        } catch (FlagParseException e) {
            sendUsageMessage(sender);
            return;
        }

        SubCommand subCommand = null;
        if (!subCommands.isEmpty()) {
            if (mustUseSubCommands() && args.isEmpty()) {
                sendUsageMessage(sender);
                return;
            }

            if (!args.isEmpty()) subCommand = getSubCommand(args.getFirst());

            if (subCommand == null && mustUseSubCommands()) {
                sendUsageMessage(sender);
                return;
            } else if (subCommand != null) {
                args.removeFirst();
            }
        }

        Map<String, Object> subCommandFlags = new LinkedHashMap<>();
        LastParsed currentParameter = new LastParsed();
        Map<String, Object> parsedArgs;
        try {
            if (subCommand == null) {
                parsedArgs = parseArgs(args, currentParameter);
            } else {
                ScanResult subFlags = subCommand.scanAndCleanFlags(args, true);
                subCommandFlags = subFlags.regularFlags();
                parsedArgs = subCommand.parseArgs(args, currentParameter);
            }

        } catch (FlagParseException | NoArgumentFoundException e) {
            sendUsageMessage(sender, subCommand);
            return;
        } catch (ArgumentParseException e) {
            if (currentParameter.value != null && currentParameter.value.getCustomErrorMessage() != null) {
                sender.warn(currentParameter.value.getCustomErrorMessage());
            } else {
                sendUsageMessage(sender, subCommand);
            }

            return;
        }

        Map<String, Object> finalFlags = new LinkedHashMap<>(commandFlags.globalFlags());
        if (subCommand == null) {
            finalFlags.putAll(commandFlags.regularFlags());
        } else {
            finalFlags.putAll(subCommandFlags);
        }

        boolean success;
        if (subCommand == null) {
            success = run(sender, new CommandContext(label, parsedArgs, finalFlags));
        } else {
            success = subCommand.run(sender, new CommandContext(label, parsedArgs, finalFlags));
        }

        if (!success) {
            sendUsageMessage(sender, subCommand);
        }
    }

    public abstract boolean run(CommandSender sender, CommandContext ctx);

    private String buildUsageMessage(SubCommand subCommand) {
        if (subCommand != null) return subCommand.getUsage();
        StringBuilder usage = new StringBuilder();
        int index = 0;
        int size = subCommands.size();
        for (SubCommand cmd : subCommands.values()) {
            usage.append(cmd.getUsage());
            if (++index < size) usage.append("\n");
        }

        if (!subCommands.isEmpty() && !parameters.isEmpty()) {
            usage.append("\n");
        }

        if (!mustUseSubCommands()) {
            usage.append(getName());

            for (BaseCommandParameter parameter : parameters) {
                usage.append(parameter.isOptional()
                        ? " [" + parameter.getName() + ": " + parameter.getType() + "]"
                        : " <" + parameter.getName() + ": " + parameter.getType() + ">"
                );
            }

            for (CommandFlag flag : flags.values()) {
                usage.append(" ").append(flag.buildUsage());
            }
        }

        return usage.toString();
    }

    public void sendUsageMessage(CommandSender sender, SubCommand subCommand, CloudLogLevel logLevel) {
        for (String line : getUsage(subCommand).split("\n")) {
            sender.log(logLevel != null ? logLevel : CloudLogLevel.WARN, line.trim());
        }
    }

    public void sendUsageMessage(CommandSender sender) {
        sendUsageMessage(sender, null, null);
    }

    public void sendUsageMessage(CommandSender sender, SubCommand subCommand) {
        sendUsageMessage(sender, subCommand, null);
    }

    public Command registerSubCommand(SubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(), subCommand);
        return this;
    }

    @Nullable
    public SubCommand getSubCommand(String name) {
        return subCommands.get(name.toLowerCase());
    }

    public boolean mustUseSubCommands() {
        return !subCommands.isEmpty() && parameters.isEmpty();
    }

    public String getUsage(SubCommand subCommand) {
        return subCommand != null ? buildUsageMessage(subCommand) : (usage != null ? usage : buildUsageMessage(null));
    }

    public String getUsage() {
        return getUsage(null);
    }
}