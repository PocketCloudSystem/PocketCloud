package de.pocketcloud.cloud.console.log;

import de.pocketcloud.cloud.console.log.def.MainLogger;
import de.pocketcloud.cloud.console.log.def.PrefixedLogger;

public final class CloudLogger {

    private static final de.pocketcloud.api.logging.ILogger MAIN = new MainLogger();
    private static volatile de.pocketcloud.api.logging.ILogger instance = MAIN;

    private CloudLogger() {}

    public static void set(de.pocketcloud.api.logging.ILogger logger) {
        instance = logger != null ? logger : MAIN;
    }

    public static de.pocketcloud.api.logging.ILogger get() {
        return instance;
    }

    public static PrefixedLogger prefixed(String prefix) {
        return new PrefixedLogger(get(), prefix);
    }
}