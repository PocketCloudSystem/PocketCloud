package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class EnabledPluginParameter extends BaseCommandParameter {

    public EnabledPluginParameter(String name, boolean optional) {
        super(name, optional);
    }

    public EnabledPluginParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        return PocketCloud.instance().plugins().getEnabledPlugins().stream()
                .filter(p -> p.getDescription().name().equals(input))
                .findFirst()
                .orElseThrow(() -> new ArgumentParseException("Plugin not found"));
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return PocketCloud.instance().plugins().getEnabledPlugins().stream().map(p -> p.getDescription().name()).toList();
    }

    @Override
    public String getType() {
        return "enabled_plugin";
    }
}