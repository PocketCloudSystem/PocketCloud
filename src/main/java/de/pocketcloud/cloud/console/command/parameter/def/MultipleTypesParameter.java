package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class MultipleTypesParameter extends BaseCommandParameter {

    private final List<BaseCommandParameter> allowedTypes;

    public MultipleTypesParameter(String name, List<BaseCommandParameter> allowedTypes, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
        this.allowedTypes = List.copyOf(allowedTypes);
    }

    public MultipleTypesParameter(String name, boolean optional, BaseCommandParameter... allowedTypes) {
        this(name, Arrays.asList(allowedTypes), optional, null);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        for (BaseCommandParameter type : allowedTypes) {
            try {
                return type.parseValue(input);
            } catch (ArgumentParseException ignored) {}
        }
        throw new ArgumentParseException();
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        List<String> combined = new ArrayList<>();
        for (BaseCommandParameter type : allowedTypes) combined.addAll(type.onTabCompleteMatch(currentArg));
        return combined.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public String getType() {
        return allowedTypes.stream().map(BaseCommandParameter::getName).collect(Collectors.joining("|"));
    }
}