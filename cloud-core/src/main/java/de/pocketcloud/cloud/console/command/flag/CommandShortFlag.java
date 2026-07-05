package de.pocketcloud.cloud.console.command.flag;

public final class CommandShortFlag extends CommandFlag {

    public static final String PREFIX = "-";
    public static final int CHARACTER_LIMIT = 1;

    public CommandShortFlag(String flag, boolean global, boolean expectValue) {
        super(PREFIX, flag, CHARACTER_LIMIT, global, expectValue);
    }

    @Override
    public boolean isLikelyFlag(String arg) {
        return arg.startsWith(PREFIX) && !arg.startsWith(CommandLongFlag.PREFIX);
    }
}