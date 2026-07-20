package de.pocketcloud.cloud.console.log.def;

import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.logging.ILogger;
import lombok.Getter;
import lombok.Setter;

public class PrefixedLogger implements ILogger {

    private final ILogger parent;
    @Getter
    @Setter
    private String prefix;

    public PrefixedLogger(ILogger parent, String prefix) {
        this.parent = parent;
        this.prefix = prefix;
    }

    @Override
    public ILogger log(CloudLogLevel level, String message, Object... params) {
        parent.log(level, prefix + " " + message, params);
        return this;
    }

    @Override
    public ILogger withoutFormat(String message, Object... params) {
        parent.withoutFormat(prefix + " " + message, params);
        return this;
    }

    @Override
    public ILogger exception(Throwable throwable) {
        return parent.exception(throwable);
    }

    @Override
    public ILogger exception(String message, Throwable throwable, Object... params) {
        return parent.exception(prefix + " " + message, throwable, params);
    }

    @Override
    public ILogger echo(String message) {
        return parent.echo(prefix + " " + message);
    }

    @Override
    public boolean isDebugMode() {
        return parent.isDebugMode();
    }

    @Override
    public ILogger setDebugMode(boolean debugMode) {
        return parent.setDebugMode(debugMode);
    }

    @Override
    public boolean isSaveLogs() {
        return parent.isSaveLogs();
    }

    @Override
    public ILogger setSaveLogs(boolean enabled) {
        return parent.setSaveLogs(enabled);
    }
}