package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;
import lombok.Getter;

@Getter
public class StringParameter extends BaseCommandParameter {

    private final boolean multiString;

    public StringParameter(String name, boolean optional, boolean multiString) {
        super(name, optional);
        this.multiString = multiString;
    }

    public StringParameter(String name, boolean optional) {
        this(name, optional, false);
    }

    @Override
    public String parseValue(String input) {
        return input;
    }

    @Override
    public String getType() {
        return "string";
    }
}