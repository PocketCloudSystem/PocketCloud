package de.pocketcloud.cloud.console.command.flag;

import lombok.Getter;

/**
 * Represents a command flag. Two variants exist:
 * Short flags:  -y            (single character, prefix "-")
 * Long flags:   --force       (up to 32 characters, prefix "--")
 * <p>
 * If {@code expectValue} is false and the flag is present, its parsed value is {@code true} (boolean toggle).
 * If {@code expectValue} is true, the flag must be followed by {@code =<value>}, e.g. {@code --timeout=30}.
 * <p>
 * Global flags ({@code global = true}) are shared across the root command and all its sub-commands.
 */
@Getter
public abstract class CommandFlag {

    private final String prefix;
    private final String flag;
    private final int characterLimit;
    private final boolean global;
    private final boolean expectValue;

    protected CommandFlag(String prefix, String flag, int characterLimit, boolean global, boolean expectValue) {
        if (flag.isEmpty() || flag.length() > characterLimit)
            throw new IllegalArgumentException("Flag '" + flag + "' must be between 1 and " + characterLimit + " characters");
        this.prefix = prefix;
        this.flag = flag;
        this.characterLimit = characterLimit;
        this.global = global;
        this.expectValue = expectValue;
    }

    public abstract boolean isLikelyFlag(String arg);

    public String buildUsage() {
        return "[" + getFullFlag() + (expectValue ? "=..." : "") + "]";
    }

    public String getFullFlag() {
        return prefix + flag;
    }

    /**
     * Creates a short flag (e.g. {@code -y}).
     */
    public static CommandShortFlag shortFlag(String flag) {
        return new CommandShortFlag(flag, false, false);
    }

    public static CommandShortFlag shortFlag(String flag, boolean global, boolean expectValue) {
        return new CommandShortFlag(flag, global, expectValue);
    }

    /**
     * Creates a long flag (e.g. {@code --force}).
     */
    public static CommandLongFlag longFlag(String flag) {
        return new CommandLongFlag(flag, false, false);
    }

    public static CommandLongFlag longFlag(String flag, boolean global, boolean expectValue) {
        return new CommandLongFlag(flag, global, expectValue);
    }
}