package de.pocketcloud.cloud.console.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.pocketcloud.api.logging.CloudLogLevel;

public final class CloudLevelConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        boolean isSuccess = event.getMarkerList() != null &&
                event.getMarkerList().stream().anyMatch(m -> m.getName().equals("SUCCESS"));

        CloudLogLevel actualLevel = isSuccess ? CloudLogLevel.SUCCESS : switch (event.getLevel().toString()) {
            case "WARN" -> CloudLogLevel.WARN;
            case "ERROR" -> CloudLogLevel.ERROR;
            case "DEBUG" -> CloudLogLevel.DEBUG;
            default -> CloudLogLevel.INFO;
        };

        return CloudLogLevel.padPrefixToLength(actualLevel);
    }
}