package de.pocketcloud.bridge.platform.pnx;

import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.common.util.FormatUtils;
import org.powernukkitx.plugin.PluginLogger;
import org.powernukkitx.utils.LogLevel;

public final class PowerNukkitXLogger implements ILogger {

    private final PluginLogger logger;

    public PowerNukkitXLogger(PluginLogger logger) {
        this.logger = logger;
    }

    @Override
    public ILogger exception(Throwable throwable) {
        logger.error(throwable.getMessage(), throwable);
        return this;
    }

    @Override
    public ILogger exception(String message, Throwable throwable, Object... params) {
        logger.error(FormatUtils.interpolate(message, params), throwable);
        return this;
    }

    @Override
    public ILogger log(CloudLogLevel logLevel, String message, Object... params) {
        LogLevel adaptedLevel = switch (logLevel) {
            case INFO -> LogLevel.INFO;
            case WARN -> LogLevel.WARNING;
            case ERROR -> LogLevel.ERROR;
            case SUCCESS -> LogLevel.NOTICE;
            case DEBUG -> LogLevel.DEBUG;
        };

        logger.log(adaptedLevel, FormatUtils.interpolate(message, params));
        return this;
    }

    @Override
    public ILogger withoutFormat(String message, Object... params) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ILogger echo(String message) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ILogger setDebugMode(boolean debugMode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDebugMode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ILogger setSaveLogs(boolean enabled) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSaveLogs() {
        throw new UnsupportedOperationException();
    }
}