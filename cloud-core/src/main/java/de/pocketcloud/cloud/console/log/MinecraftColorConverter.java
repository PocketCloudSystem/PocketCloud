package de.pocketcloud.cloud.console.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import de.pocketcloud.cloud.console.ConsoleColor;

public final class MinecraftColorConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(ILoggingEvent event, String in) {
        return ConsoleColor.convert(in);
    }
}