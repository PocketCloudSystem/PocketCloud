package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class ServerParameter extends BaseCommandParameter {

    public ServerParameter(String name, boolean optional) {
        super(name, optional);
    }

    public ServerParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        return PocketCloud.instance().servers().get(input).orElseThrow(() -> new ArgumentParseException("Server not found"));
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return PocketCloud.instance().servers().getAll().stream().map(ICloudServer::name).toList();
    }

    @Override
    public String getType() {
        return "server";
    }
}