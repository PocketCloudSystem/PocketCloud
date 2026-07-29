package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class ServerGroupParameter extends BaseCommandParameter {

    public ServerGroupParameter(String name, boolean optional) {
        super(name, optional);
    }

    public ServerGroupParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        return PocketCloud.instance().serverGroups().get(input).orElseThrow(() -> new ArgumentParseException("ServerGroup not found"));
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return PocketCloud.instance().serverGroups().getAll().stream().map(IServerGroup::name).toList();
    }

    @Override
    public String getType() {
        return "server_group";
    }
}