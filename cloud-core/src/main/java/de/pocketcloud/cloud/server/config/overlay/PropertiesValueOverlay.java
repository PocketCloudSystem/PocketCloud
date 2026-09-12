package de.pocketcloud.cloud.server.config.overlay;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PropertiesValueOverlay {

    private static final Pattern LINE_PATTERN = Pattern.compile("^(\\s*)([^\\s#!=:][^=:]*?)(\\s*[=:]\\s*)(.*)$");

    private PropertiesValueOverlay() {}

    public static String overlay(String remoteRaw, String oldLocalRaw) {
        Map<String, String> oldValues = extractValues(oldLocalRaw);
        if (oldValues.isEmpty()) return remoteRaw;

        List<String> remoteLines = splitLines(remoteRaw);
        List<String> output = new ArrayList<>();

        for (String line : remoteLines) {
            if (isComment(line)) {
                output.add(line);
                continue;
            }

            Matcher matcher = LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                output.add(line);
                continue;
            }

            String indentStr = matcher.group(1);
            String key = matcher.group(2).strip();
            String separator = matcher.group(3);
            String value = matcher.group(4);

            String oldValue = oldValues.get(key);
            if (oldValue != null && !oldValue.equals(value.strip())) {
                output.add(indentStr + key + separator + oldValue);
            } else {
                output.add(line);
            }
        }

        return String.join("\n", output);
    }

    private static Map<String, String> extractValues(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : splitLines(raw)) {
            if (isComment(line)) continue;
            Matcher matcher = LINE_PATTERN.matcher(line);
            if (!matcher.matches()) continue;
            values.put(matcher.group(2).strip(), matcher.group(4).strip());
        }
        return values;
    }

    private static boolean isComment(String line) {
        String trimmed = line.strip();
        return trimmed.startsWith("#") || trimmed.startsWith("!");
    }

    private static List<String> splitLines(String raw) {
        return new ArrayList<>(Arrays.asList(raw.split("\n", -1)));
    }
}