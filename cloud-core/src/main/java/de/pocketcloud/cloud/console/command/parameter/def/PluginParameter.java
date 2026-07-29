package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class PluginParameter extends BaseCommandParameter {

    public PluginParameter(String name, boolean optional) {
        super(name, optional);
    }

    public PluginParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        return PocketCloud.instance().plugins().get(input).orElseThrow(() -> new ArgumentParseException("Plugin not found"));
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return PocketCloud.instance().plugins().getPlugins().values().stream().map(p -> p.getDescription().name()).toList();
    }

    @Override
    public String getType() {
        return "plugin";
    }
}