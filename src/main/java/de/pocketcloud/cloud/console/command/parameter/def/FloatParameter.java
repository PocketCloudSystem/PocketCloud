package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;

public class FloatParameter extends BaseCommandParameter {

    public FloatParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    public FloatParameter(String name, boolean optional) {
        super(name, optional);
    }

    @Override
    public Float parseValue(String input) throws ArgumentParseException {
        try {
            return Float.parseFloat(input);
        } catch (NumberFormatException e) {
            throw new ArgumentParseException();
        }
    }

    @Override
    public String getType() {
        return "float";
    }
}