package de.pocketcloud.cloud.console.command.flag;

public final class CommandLongFlag extends CommandFlag {

    public static final String PREFIX = "--";
    public static final int CHARACTER_LIMIT = 32;

    public CommandLongFlag(String flag, boolean global, boolean expectValue) {
        super(PREFIX, flag, CHARACTER_LIMIT, global, expectValue);
    }

    @Override
    public boolean isLikelyFlag(String arg) {
        return arg.startsWith(PREFIX);
    }
}