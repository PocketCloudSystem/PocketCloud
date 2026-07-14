package de.pocketcloud.cloud.console.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;
import de.pocketcloud.cloud.console.ConsoleColor;
import de.pocketcloud.cloud.console.log.cache.LogMessagesCache;
import de.pocketcloud.cloud.console.output.OutputManager;
import lombok.Setter;

@Setter
public final class MinecraftConsoleAppender extends AppenderBase<ILoggingEvent> {

    private Layout<ILoggingEvent> layout;

    @Override
    protected void append(ILoggingEvent event) {
        String formatted = layout.doLayout(event);
        LogMessagesCache.save(ConsoleColor.clean(formatted));
        OutputManager.get().handleOutput(formatted);
    }
}