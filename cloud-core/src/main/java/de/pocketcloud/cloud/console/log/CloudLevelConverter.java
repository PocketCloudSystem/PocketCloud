package de.pocketcloud.cloud.console.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public final class CloudLevelConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        boolean isSuccess = event.getMarkerList() != null &&
                event.getMarkerList().stream().anyMatch(m -> m.getName().equals("SUCCESS"));

        if (isSuccess) return de.pocketcloud.api.logging.CloudLogLevel.SUCCESS.prefix();

        return switch (event.getLevel().toString()) {
            case "WARN" -> de.pocketcloud.api.logging.CloudLogLevel.WARN.prefix();
            case "ERROR" -> de.pocketcloud.api.logging.CloudLogLevel.ERROR.prefix();
            case "DEBUG" -> de.pocketcloud.api.logging.CloudLogLevel.DEBUG.prefix();
            default -> de.pocketcloud.api.logging.CloudLogLevel.INFO.prefix();
        };
    }
}