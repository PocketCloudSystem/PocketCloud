package de.pocketcloud.cloud.plugin.exception;

public final class PluginLoadFailedException extends RuntimeException {

    public PluginLoadFailedException() {}

    public PluginLoadFailedException(String message) {
        super(message);
    }

    public PluginLoadFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginLoadFailedException(Throwable cause) {
        super(cause);
    }

    public PluginLoadFailedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}