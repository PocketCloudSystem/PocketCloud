package de.pocketcloud.cloud.console;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.jline.jansi.Ansi;

import java.util.Arrays;

@Getter
@Accessors(fluent = true)
public enum ConsoleColor {

    BLACK('0', Ansi.ansi().fg(Ansi.Color.BLACK)),
    DARK_BLUE('1', Ansi.ansi().fg(Ansi.Color.BLUE)),
    DARK_GREEN('2', Ansi.ansi().fg(Ansi.Color.GREEN)),
    DARK_AQUA('3', Ansi.ansi().fg(Ansi.Color.CYAN)),
    DARK_RED('4', Ansi.ansi().fg(Ansi.Color.RED)),
    DARK_PURPLE('5', Ansi.ansi().fg(Ansi.Color.MAGENTA)),
    ORANGE('6', Ansi.ansi().fg(Ansi.Color.YELLOW)),
    GRAY('7', Ansi.ansi().fg(Ansi.Color.WHITE)),
    DARK_GRAY('8', Ansi.ansi().fgBright(Ansi.Color.BLACK)),
    BLUE('9', Ansi.ansi().fgBright(Ansi.Color.BLUE)),
    GREEN('a', Ansi.ansi().fgBright(Ansi.Color.GREEN)),
    AQUA('b', Ansi.ansi().fgBright(Ansi.Color.CYAN)),
    RED('c', Ansi.ansi().fgBright(Ansi.Color.RED)),
    LIGHT_PURPLE('d', Ansi.ansi().fgBright(Ansi.Color.MAGENTA)),
    YELLOW('e', Ansi.ansi().fgBright(Ansi.Color.YELLOW)),
    WHITE('f', Ansi.ansi().fgBright(Ansi.Color.WHITE)),
    GOLD('g', Ansi.ansi().fg(Ansi.Color.YELLOW)),
    BOLD('l', Ansi.ansi().bold()),
    ITALIC('o', Ansi.ansi().a(Ansi.Attribute.ITALIC)),
    RESET('r', Ansi.ansi().reset());

    public static final char SYMBOL = '§';
    private final char colorCode;
    private final Ansi ansiCode;

    ConsoleColor(char colorCode, Ansi ansiCode) {
        this.colorCode = colorCode;
        this.ansiCode = ansiCode;
    }

    public static String convert(String input) {
        if (input == null) return null;

        StringBuilder result = new StringBuilder();
        String[] parts = input.split(String.valueOf(SYMBOL));

        result.append(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) continue;

            char code = Character.toLowerCase(part.charAt(0));
            String remaining = part.substring(1);

            ConsoleColor color = fromCode(code);
            if (color != null && color.ansiCode() != null) {
                result.append(color.ansiCode());
            }
            result.append(remaining);
        }

        result.append(ConsoleColor.RESET.ansiCode());
        return result.toString();
    }

    public static String clean(String input) {
        if (input == null) return null;
        return input.replaceAll(SYMBOL + "[0-9a-ur-t]", "");
    }

    private static ConsoleColor fromCode(char code) {
        return Arrays.stream(values()).filter(consoleColor -> consoleColor.colorCode() == code).findAny().orElse(null);
    }
}