package de.pocketcloud.cloud.console.command.sender;

import de.pocketcloud.cloud.console.log.CloudLogger;

public final class ConsoleCommandSender implements CommandSender {

    @Override
    public ConsoleCommandSender info(String message, Object... params) {
        CloudLogger.get().info(message, params);
        return this;
    }

    @Override
    public ConsoleCommandSender warn(String message, Object... params) {
        CloudLogger.get().warn(message, params);
        return this;
    }

    @Override
    public ConsoleCommandSender error(String message, Object... params) {
        CloudLogger.get().error(message, params);
        return this;
    }

    @Override
    public ConsoleCommandSender success(String message, Object... params) {
        CloudLogger.get().success(message, params);
        return this;
    }

    @Override
    public ConsoleCommandSender debug(String message, Object... params) {
        CloudLogger.get().debug(message, params);
        return this;
    }
}