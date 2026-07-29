package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class TemplateParameter extends BaseCommandParameter {

    public TemplateParameter(String name, boolean optional) {
        super(name, optional);
    }

    public TemplateParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        return PocketCloud.instance().templates().get(input).orElseThrow(() -> new ArgumentParseException("Template not found"));
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return PocketCloud.instance().templates().getAll().stream().map(ITemplate::name).toList();
    }

    @Override
    public String getType() {
        return "template";
    }
}