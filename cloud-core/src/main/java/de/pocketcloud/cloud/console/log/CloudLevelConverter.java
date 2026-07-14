package de.pocketcloud.cloud.console.log;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

public final class CloudLevelConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        boolean isSuccess = event.getMarkerList() != null &&
                event.getMarkerList().stream().anyMatch(m -> m.getName().equals("SUCCESS"));

        if (isSuccess) return CloudLogLevel.SUCCESS.prefix();

        return switch (event.getLevel().toString()) {
            case "WARN" -> CloudLogLevel.WARN.prefix();
            case "ERROR" -> CloudLogLevel.ERROR.prefix();
            case "DEBUG" -> CloudLogLevel.DEBUG.prefix();
            default -> CloudLogLevel.INFO.prefix();
        };
    }
}