package de.pocketcloud.cloud.console.log.def;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.cloud.console.ConsoleColor;
import de.pocketcloud.cloud.console.log.cache.LogMessagesCache;
import de.pocketcloud.cloud.console.output.OutputManager;
import de.pocketcloud.common.util.FormatUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class MainLogger implements ILogger {

    private static final Logger SLF4J = (Logger) LoggerFactory.getLogger("PocketCloud");
    private static final Marker SUCCESS_MARKER = MarkerFactory.getMarker("SUCCESS");

    private boolean saveLogs = true;

    public MainLogger() {
        this(false);
    }

    public MainLogger(boolean debugMode) {
        setDebugMode(debugMode);
    }

    @Override
    public ILogger log(CloudLogLevel level, String message, Object... params) {
        String parsed = params.length > 0 ? FormatUtils.interpolate(message, params) : message;

        switch (level) {
            case WARN -> SLF4J.warn(parsed);
            case ERROR -> SLF4J.error(parsed);
            case SUCCESS -> SLF4J.info(SUCCESS_MARKER, parsed);
            case DEBUG -> SLF4J.debug(parsed);
            default -> SLF4J.info(parsed);
        }
        return this;
    }

    @Override
    public ILogger exception(Throwable throwable) {
        SLF4J.error("Unhandled exception", throwable);
        return this;
    }

    @Override
    public ILogger exception(String message, Throwable throwable, Object... params) {
        String parsed = params.length > 0 ? FormatUtils.interpolate(message, params) : message;
        SLF4J.error(parsed, throwable);
        return this;
    }

    @Override
    public ILogger withoutFormat(String message, Object... params) {
        message = FormatUtils.interpolate(message, params);
        return echo(message);
    }

    @Override
    public ILogger echo(String message) {
        if (saveLogs) LogMessagesCache.save(ConsoleColor.clean(message));
        if (OutputManager.get().canOutput(this)) OutputManager.get().handleOutput(ConsoleColor.convert(message));
        return this;
    }

    @Override
    public ILogger setDebugMode(boolean debugMode) {
        SLF4J.setLevel(debugMode ? Level.DEBUG : Level.INFO);
        return this;
    }

    @Override
    public boolean isDebugMode() {
        return SLF4J.getLevel() == Level.DEBUG || SLF4J.isDebugEnabled();
    }

    @Override
    public ILogger setSaveLogs(boolean enabled) {
        this.saveLogs = enabled;
        return this;
    }

    @Override
    public boolean isSaveLogs() {
        return saveLogs;
    }
}