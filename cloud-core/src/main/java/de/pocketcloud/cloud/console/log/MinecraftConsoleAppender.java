package de.pocketcloud.cloud.console.log;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import de.pocketcloud.cloud.console.ConsoleColor;
import de.pocketcloud.cloud.console.log.cache.LogMessagesCache;
import de.pocketcloud.cloud.console.output.OutputManager;
import lombok.Setter;

@Setter
public final class MinecraftConsoleAppender extends AppenderBase<ILoggingEvent> {

    private PatternLayoutEncoder encoder;

    @Override
    public void start() {
        if (encoder == null) {
            addError("No encoder configured for " + name);
            return;
        }

        if (!encoder.isStarted()) encoder.start();
        super.start();
    }

    @Override
    protected void append(ILoggingEvent event) {
        byte[] encoded = encoder.encode(event);
        String formatted = new String(encoded, java.nio.charset.StandardCharsets.UTF_8);

        LogMessagesCache.save(ConsoleColor.clean(formatted));
        OutputManager.get().handleOutput(ConsoleColor.convert(formatted));
    }
}