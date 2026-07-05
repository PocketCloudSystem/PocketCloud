package de.pocketcloud.cloud.console.command.ctx;

import de.pocketcloud.cloud.console.command.Command;

import java.util.Map;

public record CommandContext(String label, Map<String, Object> args, Map<String, Object> flags) {

    public boolean hasArg(String name) {
        return args.containsKey(name);
    }

    public boolean hasFlag(String name) {
        return flags.containsKey(name);
    }

    public <T> T arg(String name, Class<T> type) {
        return type.cast(args.get(name));
    }

    public String argString(String name) {
        return arg(name, String.class);
    }

    public int argInt(String name) {
        return arg(name, Integer.class);
    }

    public double argDouble(String name) {
        return arg(name, Double.class);
    }

    public float argFloat(String name) {
        return arg(name, Float.class);
    }

    public boolean argBool(String name) {
        return arg(name, Boolean.class);
    }

    public Command argCommand(String name) {
        return arg(name, Command.class);
    }

    public <T> T flag(String name, Class<T> type) {
        return type.cast(flags.get(name));
    }

    public String flagString(String name) {
        return flag(name, String.class);
    }

    public Integer flagInt(String name) {
        return flag(name, Integer.class);
    }

    public Double flagDouble(String name) {
        return flag(name, Double.class);
    }

    public Float flagFloat(String name) {
        return flag(name, Float.class);
    }

    public Boolean flagBool(String name) {
        return flag(name, Boolean.class);
    }
}