package de.pocketcloud.cloud.console.log.def;

import de.pocketcloud.cloud.console.log.CloudLogLevel;
import de.pocketcloud.cloud.console.log.ILogger;
import de.pocketcloud.cloud.util.FormatUtils;

import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainLogger implements ILogger {

    public static final String LOG_FORMAT = "§8[§b{time_with_ms}§8] §8[§r{thread}§8/§r{log_level}§r§8] §r{message}§r";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_MS_FMT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    protected boolean closed = false;
    protected String format = null;
    private BufferedWriter logFile = null;

    private boolean debugMode;
    private boolean saveLogs;

    public MainLogger(String cloudLogPath, boolean debugMode, boolean saveLogs) {
        this.debugMode = debugMode;
        this.saveLogs = saveLogs;
        if (cloudLogPath != null) {
            try {
                logFile = new BufferedWriter(new FileWriter(cloudLogPath, true));
            } catch (IOException e) {
                exception(e);
                logFile = null;
            }
        }
    }

    @Override
    public MainLogger exception(Throwable throwable) {
        if (throwable.getStackTrace().length > 0) {
            error("§cUnhandled §e{}§c: §e{} §cwas thrown in §e{} §cat line §e{}",
                    throwable.getClass().getName(),
                    throwable.getMessage(),
                    throwable.getStackTrace()[0].getFileName(),
                    throwable.getStackTrace()[0].getLineNumber()
            );
        } else {
            error("§cUnhandled §e{}§c: §e{}",
                    throwable.getClass().getName(),
                    throwable.getMessage()
            );
        }

        int i = 1;
        for (StackTraceElement trace : throwable.getStackTrace()) {
            error("§cTrace §e#{} §ccalled at '§e{}§c' in §e{} §cat line §e{}",
                    i++,
                    trace.getMethodName(),
                    trace.getClassName(),
                    trace.getLineNumber()
            );
        }

        return this;
    }

    @Override
    public ILogger exception(String message, Throwable throwable, Object... params) {
        error(message, params);
        return exception(throwable);
    }

    @Override
    public MainLogger log(CloudLogLevel logLevel, String message, Object... params) {
        LocalTime now = LocalTime.now();
        String threadName = Thread.currentThread().getName();

        String parsedMessage = params.length > 0 ? FormatUtils.interpolate(message, params) : message;
        String formatted = (format != null ? format : LOG_FORMAT)
                .replace("{thread}", threadName)
                .replace("{time}", now.format(TIME_FMT))
                .replace("{time_with_ms}", now.format(TIME_MS_FMT))
                .replace("{log_level}", logLevel.prefix())
                .replace("{message}", parsedMessage);

        echo(formatted);
        return this;
    }

    @Override
    public void appendLogEntry(String message) {
        if (closed || logFile == null) return;
        try {
            logFile.write(message);
            logFile.newLine();
            logFile.flush();
        } catch (IOException e) {
            exception(e);
        }
    }

    @Override
    public void closeLogFile() {
        if (closed || logFile == null) return;
        closed = true;
        try {
            logFile.close();
        } catch (IOException e) {
            exception(e);
        }

        logFile = null;
    }

    @Override
    public MainLogger setFormat(String format) {
        this.format = format;
        return this;
    }

    @Override
    public MainLogger resetFormat() {
        return setFormat(null);
    }

    @Override
    public String getFormat() {
        return format;
    }

    @Override
    public MainLogger setDebugMode(boolean e) {
        this.debugMode = e;
        return this;
    }

    @Override
    public boolean isDebugMode() {
        return debugMode;
    }

    @Override
    public MainLogger setSaveLogs(boolean e) {
        this.saveLogs = e;
        return this;
    }

    @Override
    public boolean isSaveLogs() {
        return saveLogs;
    }
}