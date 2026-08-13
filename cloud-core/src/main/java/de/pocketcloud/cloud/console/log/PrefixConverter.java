package de.pocketcloud.cloud.console.log;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.slf4j.MDC;

public final class PrefixConverter extends CompositeConverter<LoggingEvent> {

    @Override
    protected String transform(LoggingEvent loggingEvent, String s) {
        PrefixResult prefixResult = extractPrefix(s);
        MDC.put("cleanMsg", prefixResult.message());
        if (prefixResult.prefix == null) return " ";
        return " §8[§r" + prefixResult.prefix + "§8] ";
    }

    public static PrefixResult extractPrefix(String message) {
        if (!message.startsWith("%prefix:")) return new PrefixResult(null, message);
        int end = message.indexOf('%', 8);
        if (end == -1) return new PrefixResult(null, message);
        String prefix = message.substring(8, end);
        String cleanedMessage = message.substring(end + 1);
        return new PrefixResult(prefix, cleanedMessage);
    }

    public record PrefixResult(String prefix, String message) {}
}