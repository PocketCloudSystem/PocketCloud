package de.pocketcloud.cloud.console.log;

import de.pocketcloud.cloud.console.log.def.MainLogger;
import de.pocketcloud.cloud.console.log.def.PrefixedLogger;

public final class CloudLogger {

    private static final ILogger MAIN = new MainLogger();
    private static volatile ILogger instance = MAIN;

    private CloudLogger() {}

    public static void set(ILogger logger) {
        instance = logger != null ? logger : MAIN;
    }

    public static ILogger get() {
        return instance;
    }

    public static PrefixedLogger prefixed(String prefix) {
        return new PrefixedLogger(get(), prefix);
    }
}