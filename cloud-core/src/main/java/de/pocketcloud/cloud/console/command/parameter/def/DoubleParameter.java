package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;

public class DoubleParameter extends BaseCommandParameter {

    public DoubleParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    public DoubleParameter(String name, boolean optional) {
        super(name, optional);
    }

    @Override
    public Double parseValue(String input) throws ArgumentParseException {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new ArgumentParseException();
        }
    }

    @Override
    public String getType() {
        return "double";
    }
}