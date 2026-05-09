package de.pocketcloud.cloud.console.log;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.ConsoleColor;
import de.pocketcloud.cloud.console.log.cache.LogMessagesCache;
import org.jline.reader.LineReader;

public interface ILogger {
    
    default ILogger info(String message, Object... params) {
        return log(CloudLogLevel.INFO, message, params);
    }
    
    default ILogger warn(String message, Object... params) {
        return log(CloudLogLevel.WARN, message, params);
    }
    
    default ILogger error(String message, Object... params) {
        return log(CloudLogLevel.ERROR, message, params);
    }
    
    default ILogger success(String message, Object... params) {
        return log(CloudLogLevel.SUCCESS, message, params);
    }
    
    default ILogger debug(String message, Object... params) {
        if (isDebugMode()) return forceDebug(message, params);
        return this;
    }

    default ILogger forceDebug(String message, Object... params) {
        return log(CloudLogLevel.DEBUG, message, params);
    }

    ILogger exception(Throwable throwable);

    ILogger exception(String message, Throwable throwable, Object... params);

    ILogger log(CloudLogLevel logLevel, String message, Object... params);

    default ILogger emptyLine() {
        echo("");
        return this;
    }

    default ILogger echo(String message) {
        if (isSaveLogs()) {
            String cleanedMessage = ConsoleColor.clean(message);
            LogMessagesCache.save(cleanedMessage);
            appendLogEntry(cleanedMessage);
        }

        CloudConsole console = PocketCloud.getInstance().console();
        if (console != null) {
            LineReader reader = console.getReader();
            if (reader != null) {
                reader.printAbove(message);
            } else System.out.println(message);
        } else System.out.println(message);
        return this;
    }

    void appendLogEntry(String message);

    void closeLogFile();

    ILogger setFormat(String format);

    ILogger resetFormat();

    String getFormat();

    ILogger setDebugMode(boolean debugMode);

    boolean isDebugMode();

    ILogger setSaveLogs(boolean enabled);

    boolean isSaveLogs();
}