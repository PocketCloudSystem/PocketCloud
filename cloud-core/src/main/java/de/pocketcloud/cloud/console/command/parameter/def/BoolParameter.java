package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public class BoolParameter extends BaseCommandParameter {

    public BoolParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    public BoolParameter(String name, boolean optional) {
        super(name, optional);
    }

    @Override
    public Boolean parseValue(String input) {
        return input.equalsIgnoreCase("true") || input.equalsIgnoreCase("yes");
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return List.of("true", "false");
    }

    @Override
    public String getType() {
        return "boolean";
    }
}