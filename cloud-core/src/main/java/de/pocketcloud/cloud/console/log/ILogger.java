package de.pocketcloud.cloud.console.log;

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
        return log(CloudLogLevel.DEBUG, message, params);
    }

    ILogger exception(Throwable throwable);

    ILogger exception(String message, Throwable throwable, Object... params);

    ILogger log(CloudLogLevel logLevel, String message, Object... params);

    ILogger withoutFormat(String message, Object... params);

    default ILogger emptyLine() {
        return echo("");
    }

    ILogger echo(String message);

    ILogger setDebugMode(boolean debugMode);

    boolean isDebugMode();

    ILogger setSaveLogs(boolean enabled);

    boolean isSaveLogs();
}