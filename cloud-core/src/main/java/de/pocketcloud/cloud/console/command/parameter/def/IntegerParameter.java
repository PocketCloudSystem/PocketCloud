package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;

import java.util.function.UnaryOperator;

public class IntegerParameter extends BaseCommandParameter {

    private final UnaryOperator<Integer> processFunction;

    public IntegerParameter(String name, boolean optional, UnaryOperator<Integer> processFunction, String customErrorMessage) {
        super(name, optional, customErrorMessage);
        this.processFunction = processFunction;
    }

    public IntegerParameter(String name, boolean optional, UnaryOperator<Integer> processFunction) {
        this(name, optional, processFunction, null);
    }

    public IntegerParameter(String name, boolean optional) {
        this(name, optional, null, null);
    }

    @Override
    public Integer parseValue(String input) throws ArgumentParseException {
        try {
            int value = Integer.parseInt(input);
            return processFunction != null ? processFunction.apply(value) : value;
        } catch (NumberFormatException e) {
            throw new ArgumentParseException();
        }
    }

    @Override
    public String getType() {
        return "integer";
    }
}