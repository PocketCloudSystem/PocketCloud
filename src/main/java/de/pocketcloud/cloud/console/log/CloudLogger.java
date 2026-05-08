package de.pocketcloud.cloud.console.log;

import de.pocketcloud.cloud.console.log.def.MainLogger;

public final class CloudLogger {

    private static volatile ILogger instance = null;

    public static void set(ILogger logger) {
        if (logger == null) {
            instance = null;
            return;
        }

        instance = logger;
    }

    public static ILogger get() {
        if (instance == null) instance = new MainLogger(null, false, false);
        return instance;
    }

    public static ILogger tmp() {
        return new MainLogger(null, true, false);
    }

    public static ILogger tmp(String logPath) {
        return new MainLogger(logPath, true, false);
    }

    public static ILogger tmp(String logPath, boolean debugMode, boolean saveLogs) {
        return new MainLogger(logPath, debugMode, saveLogs);
    }
}