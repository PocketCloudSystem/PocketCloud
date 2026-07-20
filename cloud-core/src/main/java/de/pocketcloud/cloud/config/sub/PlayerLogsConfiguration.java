package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.shared.network.packet.type.NotificationType;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class PlayerLogsConfiguration extends OkaeriConfig implements ICloudConfig {

    @CustomKey("connection_lifecycle")
    @Comment({"Regular player join/leave messages"})
    private ConsoleInGameConfiguration connectionLifecycle = new ConsoleInGameConfiguration();

    @CustomKey("failed_joins")
    @Comment({"Fires when a player gets kicked during login sequence"})
    private ConsoleInGameConfiguration failedJoins = new ConsoleInGameConfiguration();

    @Comment({"Regular kick via ingame or cloud messages"})
    private ConsoleInGameConfiguration kicks = new ConsoleInGameConfiguration();

    @CustomKey("server_switched")
    @Comment({"Regular player server switching messages"})
    private ConsoleInGameConfiguration serverSwitched = new ConsoleInGameConfiguration();

    @Override
    public void validate() {
        connectionLifecycle.validate();
        failedJoins.validate();
        kicks.validate();
        serverSwitched.validate();
    }

    public boolean canNotify(NotificationType type) {
        return switch (type) {
            case PLAYER_JOINED, PLAYER_LEFT -> connectionLifecycle.inGame;
            case PLAYER_JOIN_FAILED -> failedJoins.inGame;
            case PLAYER_KICKED -> kicks.inGame;
            case PLAYER_SWITCHED_SERVER -> serverSwitched.inGame;
            default -> true;
        };
    }

    public boolean canLog(NotificationType type) {
        return switch (type) {
            case PLAYER_JOINED, PLAYER_LEFT -> connectionLifecycle.console;
            case PLAYER_JOIN_FAILED -> failedJoins.console;
            case PLAYER_KICKED -> kicks.console;
            case PLAYER_SWITCHED_SERVER -> serverSwitched.console;
            default -> true;
        };
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class ConsoleInGameConfiguration extends OkaeriConfig implements ICloudConfig {

        private boolean console = true;

        @CustomKey("in-game")
        private boolean inGame = true;

        @Override
        public void validate() {}
    }
}