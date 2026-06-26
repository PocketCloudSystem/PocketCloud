package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.cloud.console.log.CloudLogLevel;
import de.pocketcloud.cloud.util.Writable;

public enum LogType implements Writable<String> {

    INFO,
    WARN,
    ERROR,
    SUCCESS,
    DEBUG;

    public CloudLogLevel toLogLevel() {
        return switch (this) {
            case INFO -> CloudLogLevel.INFO;
            case WARN -> CloudLogLevel.WARN;
            case ERROR -> CloudLogLevel.ERROR;
            case SUCCESS -> CloudLogLevel.SUCCESS;
            case DEBUG -> CloudLogLevel.DEBUG;
        };
    }

    @Override
    public String write() {
        return name();
    }
}