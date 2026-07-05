package de.pocketcloud.cloud.console.output.util;

import de.pocketcloud.cloud.console.log.ILogger;

import java.util.ArrayList;
import java.util.List;

public abstract class AuthorizedLoggerBase {

    protected final List<ILogger> authorizedLoggers = new ArrayList<>();

    public void add(ILogger logger) {
        authorizedLoggers.add(logger);
    }

    public void remove(ILogger logger) {
        authorizedLoggers.remove(logger);
    }

    public boolean isAuthorized(ILogger logger) {
        return authorizedLoggers.contains(logger);
    }
}