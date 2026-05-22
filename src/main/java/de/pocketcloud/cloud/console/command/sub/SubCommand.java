package de.pocketcloud.cloud.console.command.sub;

import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.desc.SubCommandDescription;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.holder.CommandUtilityHolder;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.util.QuadFunction;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.function.Consumer;

public abstract class SubCommand extends CommandUtilityHolder {

    @Setter
    private Command parent = null;

    @Getter
    private final String name;
    private final String usage;
    @Getter
    private final boolean optional;

    public SubCommand() {
        if (!this.getClass().isAnnotationPresent(SubCommandDescription.class)) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " requires @CommandDescription");
        }

        SubCommandDescription annotation = getClass().getAnnotation(SubCommandDescription.class);
        name = annotation.name();
        if (!annotation.usage().isBlank()) usage = annotation.usage();
        else usage = null;
        optional = annotation.optional();

        prepare();
    }

    public SubCommand(String name, String usage, boolean optional) {
        this.name = name;
        this.usage = usage;
        this.optional = optional;
        prepare();
    }

    abstract public void prepare();

    abstract public boolean run(CommandSender sender, String label, Map<String, Object> args, Map<String, Object> flags);

    private String buildUsageMessage() {
        StringBuilder usage = new StringBuilder();
        usage.append(parent != null ? parent.getName() : "<parent command>").append(" ").append(getName());
        for (BaseCommandParameter parameter : parameters) {
            usage.append(parameter.isOptional()
                    ? " [" + parameter.getName() + ": " + parameter.getType() + "]"
                    : " <" + parameter.getName() + ": " + parameter.getType() + ">"
            );
        }

        if (parent != null && !parent.getFlags().isEmpty()) {
            for (CommandFlag flag : parent.getFlags()) {
                if (flag.isGlobal()) usage.append(" ").append(flag.buildUsage());
            }
        }

        for (CommandFlag flag : getFlags()) usage.append(" ").append(flag.buildUsage());
        return usage.toString();
    }

    public String getUsage() {
        return usage != null ? usage : buildUsageMessage();
    }

    public static LambdaSubCommand lambda(String name, QuadFunction<CommandSender, String, Map<String, Object>, Map<String, Object>, Boolean> expressionCallback) {
        return new LambdaSubCommand(name, null, false, expressionCallback, null);
    }

    public static LambdaSubCommand lambda(String name, QuadFunction<CommandSender, String, Map<String, Object>, Map<String, Object>, Boolean> expressionCallback, boolean optional) {
        return new LambdaSubCommand(name, null, optional, expressionCallback, null);

    }

    public static LambdaSubCommand lambda(String name, QuadFunction<CommandSender, String, Map<String, Object>, Map<String, Object>, Boolean> expressionCallback, boolean optional, String usage) {
        return new LambdaSubCommand(name, usage, optional, expressionCallback, null);

    }

    public static LambdaSubCommand lambda(String name, QuadFunction<CommandSender, String, Map<String, Object>, Map<String, Object>, Boolean> expressionCallback, boolean optional, Consumer<SubCommand> prepareCallback) {
        return new LambdaSubCommand(name, null, optional, expressionCallback, prepareCallback);

    }

    public static LambdaSubCommand lambda(String name, QuadFunction<CommandSender, String, Map<String, Object>, Map<String, Object>, Boolean> expressionCallback, boolean optional, Consumer<SubCommand> prepareCallback, String usage) {
        return new LambdaSubCommand(name, usage, optional, expressionCallback, prepareCallback);
    }
}