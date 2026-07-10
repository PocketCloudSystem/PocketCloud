package de.pocketcloud.cloud.console.log;

import de.pocketcloud.network.packet.type.LogType;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum CloudLogLevel {

    INFO("§bINFO"),
    WARN("§cWARN"),
    ERROR("§4ERROR"),
    SUCCESS("§aSUCCESS"),
    DEBUG("§6DEBUG");

    private final String prefix;

    CloudLogLevel(String prefix) {
        this.prefix = prefix;
    }

    public static CloudLogLevel toLogLevel(LogType type) {
        return switch (type) {
            case INFO -> CloudLogLevel.INFO;
            case WARN -> CloudLogLevel.WARN;
            case ERROR -> CloudLogLevel.ERROR;
            case SUCCESS -> CloudLogLevel.SUCCESS;
            case DEBUG -> CloudLogLevel.DEBUG;
        };
    }
}