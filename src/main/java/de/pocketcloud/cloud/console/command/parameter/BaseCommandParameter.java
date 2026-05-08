package de.pocketcloud.cloud.console.command.parameter;

import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import lombok.Getter;

import java.util.List;

@Getter
public abstract class BaseCommandParameter {

    private final String name;
    private final boolean optional;
    private final String customErrorMessage;

    protected BaseCommandParameter(String name, boolean optional, String customErrorMessage) {
        this.name = name;
        this.optional = optional;
        this.customErrorMessage = customErrorMessage;
    }

    protected BaseCommandParameter(String name, boolean optional) {
        this(name, optional, null);
    }

    public abstract Object parseValue(String input) throws ArgumentParseException;

    public List<String> onTabCompleteMatch(String currentArg) {
        return List.of();
    }

    public abstract String getType();
}