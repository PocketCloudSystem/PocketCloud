package de.pocketcloud.bridge.platform.wdpe;

import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.common.util.FormatUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public final class WaterdogPELogger implements ILogger {

    private final Logger logger;

    public WaterdogPELogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public ILogger exception(Throwable throwable) {
        logger.throwing(throwable);
        return this;
    }

    @Override
    public ILogger exception(String message, Throwable throwable, Object... params) {
        logger.error(FormatUtils.interpolate(message, params), throwable);
        return this;
    }

    @Override
    public ILogger log(CloudLogLevel logLevel, String message, Object... params) {
        Level adaptedLevel = switch (logLevel) {
            case INFO, SUCCESS -> Level.INFO;
            case WARN -> Level.WARN;
            case ERROR -> Level.ERROR;
            case DEBUG -> Level.DEBUG;
        };

        logger.log(adaptedLevel, FormatUtils.interpolate(message, params));
        return this;
    }

    @Override
    public ILogger withoutFormat(String message, Object... params) {
        throw new NotImplementedException();
    }

    @Override
    public ILogger echo(String message) {
        throw new NotImplementedException();
    }

    @Override
    public ILogger setDebugMode(boolean debugMode) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isDebugMode() {
        throw new NotImplementedException();
    }

    @Override
    public ILogger setSaveLogs(boolean enabled) {
        throw new NotImplementedException();
    }

    @Override
    public boolean isSaveLogs() {
        throw new NotImplementedException();
    }
}