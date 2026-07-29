package de.pocketcloud.shared.logging;

import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.shared.network.packet.type.LogType;

public final class CloudLogLevelHelper {

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