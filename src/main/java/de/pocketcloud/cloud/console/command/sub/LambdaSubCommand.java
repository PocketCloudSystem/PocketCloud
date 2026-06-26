package de.pocketcloud.cloud.console.command.sub;

import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.sender.CommandSender;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class LambdaSubCommand extends SubCommand {

    private final BiFunction<CommandSender, CommandContext, Boolean> expressionCallback;

    public LambdaSubCommand(String name, String usage, boolean optional, BiFunction<CommandSender, CommandContext, Boolean> expressionCallback, Consumer<SubCommand> prepareCallback) {
        super(name, usage, optional);
        this.expressionCallback = expressionCallback;

        if (prepareCallback != null) {
            prepareCallback.accept(this);
        }
    }

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return expressionCallback.apply(sender, ctx);
    }
}