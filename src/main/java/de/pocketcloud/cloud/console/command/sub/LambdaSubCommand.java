package de.pocketcloud.cloud.console.command.sub;

import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.util.QuadFunction;

import java.util.Map;
import java.util.function.Consumer;

public final class LambdaSubCommand extends SubCommand {

    private final QuadFunction<CommandSender, String, Map<String, Object>, Map<String, Object>, Boolean> expressionCallback;

    public LambdaSubCommand(String name, String usage, boolean optional, QuadFunction<CommandSender, String, Map<String, Object>, Map<String, Object>, Boolean> expressionCallback, Consumer<SubCommand> prepareCallback) {
        super(name, usage, optional);
        this.expressionCallback = expressionCallback;

        if (prepareCallback != null) {
            prepareCallback.accept(this);
        }
    }

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, String label, Map<String, Object> args, Map<String, Object> flags) {
        return expressionCallback.apply(sender, label, args, flags);
    }
}