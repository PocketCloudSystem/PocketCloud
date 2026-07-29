package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class SoftwareParameter extends BaseCommandParameter {

    public SoftwareParameter(String name, boolean optional) {
        super(name, optional);
    }

    public SoftwareParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        return PocketCloud.instance().softwares().get(input).orElseThrow(() -> new ArgumentParseException("Software not found"));
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return PocketCloud.instance().softwares().getAll().stream().map(IServerSoftware::name).toList();
    }

    @Override
    public String getType() {
        return "software";
    }
}