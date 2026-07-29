package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class TemplateTypeParameter extends BaseCommandParameter {

    public TemplateTypeParameter(String name, boolean optional) {
        super(name, optional);
    }

    public TemplateTypeParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        try {
            return TemplateType.valueOf(input);
        } catch (IllegalArgumentException _) {
            throw new ArgumentParseException("TemplateType not found");
        }
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return List.of(TemplateType.SERVER.name(), TemplateType.PROXY.name());
    }

    @Override
    public String getType() {
        return "template_type";
    }
}