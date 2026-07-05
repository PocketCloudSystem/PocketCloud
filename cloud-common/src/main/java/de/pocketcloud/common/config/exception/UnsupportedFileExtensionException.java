package de.pocketcloud.common.config.exception;

public final class UnsupportedFileExtensionException extends Exception {

    public UnsupportedFileExtensionException() {
        super();
    }

    public UnsupportedFileExtensionException(String message) {
        super(message);
    }

    public UnsupportedFileExtensionException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedFileExtensionException(Throwable cause) {
        super(cause);
    }

    public UnsupportedFileExtensionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}