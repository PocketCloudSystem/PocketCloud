package de.pocketcloud.cloud.console.log.def;

import de.pocketcloud.cloud.console.log.CloudLogLevel;
import lombok.Getter;
import lombok.Setter;

public class PrefixedLogger extends MainLogger {

    private final MainLogger parentLogger;
    @Setter
    @Getter
    private String prefix;

    public PrefixedLogger(MainLogger parentLogger, String prefix) {
        super(null, false, false);
        this.parentLogger = parentLogger;
        this.prefix = prefix;
        this.closeLogFile();
    }

    @Override
    public PrefixedLogger log(CloudLogLevel logLevel, String message, Object... params) {
        parentLogger.log(logLevel, prefix + " " + message, params);
        return this;
    }
}