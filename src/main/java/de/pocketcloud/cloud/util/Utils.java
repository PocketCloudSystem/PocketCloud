package de.pocketcloud.cloud.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Utils {

    private static final Pattern PARSE_PATTERN = Pattern.compile("\"((?:\\\\.|[^\\\\\"])*)\"|(\\S+)");

    public static float formatNumber(float number, int precision) {
        BigDecimal bd = BigDecimal.valueOf(number).setScale(precision, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float formatNumber(float number) {
        return formatNumber(number, 0);
    }

    public static float formatNumber(double number, int precision) {
        BigDecimal bd = BigDecimal.valueOf(number).setScale(precision, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    public static float formatNumber(double number) {
        return formatNumber(number, 0);
    }

    public static List<String> parseQuoteAware(String input) {
        List<String> args = new ArrayList<>();
        Matcher matcher = PARSE_PATTERN.matcher(input);
        while (matcher.find()) {
            String quoted = matcher.group(1);
            String unquoted = matcher.group(2);

            String match = (quoted != null) ? quoted : unquoted;
            args.add(match.replaceAll("\\\\([\\\\\"])", "$1"));
        }

        return args;
    }
}