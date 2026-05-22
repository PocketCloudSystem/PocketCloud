package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class LiteralCommandParameter extends BaseCommandParameter {

    public LiteralCommandParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    public LiteralCommandParameter(String name, boolean optional) {
        super(name, optional);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        Command command = PocketCloud.getInstance().commandManager().get(input).orElse(null);
        if (command == null) throw new ArgumentParseException();
        return command;
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        List<String> commandNames = PocketCloud.getInstance().commandManager().getAll().stream().map(Command::getName).toList();
        if (currentArg.isEmpty()) return commandNames;
        return commandNames.stream().filter(s -> s.contains(currentArg)).collect(Collectors.toList());
    }

    @Override
    public String getType() {
        return "command";
    }
}