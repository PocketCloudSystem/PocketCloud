package de.pocketcloud.cloud.console.command.parameter.def;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.exception.ArgumentParseException;
import de.pocketcloud.cloud.console.command.parameter.BaseCommandParameter;

import java.util.List;

public final class PlayerParameter extends BaseCommandParameter {

    public PlayerParameter(String name, boolean optional) {
        super(name, optional);
    }

    public PlayerParameter(String name, boolean optional, String customErrorMessage) {
        super(name, optional, customErrorMessage);
    }

    @Override
    public Object parseValue(String input) throws ArgumentParseException {
        return PocketCloud.instance().players().get(input).orElseThrow(() -> new ArgumentParseException("Player not found"));
    }

    @Override
    public List<String> onTabCompleteMatch(String currentArg) {
        return PocketCloud.instance().players().getAll().stream().map(ICloudPlayer::name).toList();
    }

    @Override
    public String getType() {
        return "player";
    }
}