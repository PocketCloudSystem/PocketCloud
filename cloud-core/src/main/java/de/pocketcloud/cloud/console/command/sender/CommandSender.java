package de.pocketcloud.cloud.console.command.sender;

import de.pocketcloud.api.logging.CloudLogLevel;

public interface CommandSender {

    CommandSender info(String message, Object... params);

    CommandSender warn(String message, Object... params);

    CommandSender error(String message, Object... params);

    CommandSender success(String message, Object... params);

    CommandSender debug(String message, Object... params);

    default CommandSender log(CloudLogLevel logLevel, String message, Object... params) {
        switch (logLevel) {
            case ERROR:
                error(message, params);
                break;
            case WARN:
                warn(message, params);
                break;
            case SUCCESS:
                success(message, params);
                break;
            case DEBUG:
                debug(message, params);
                break;
            default:
                info(message, params);
                break;
        }

        return this;
    }
}