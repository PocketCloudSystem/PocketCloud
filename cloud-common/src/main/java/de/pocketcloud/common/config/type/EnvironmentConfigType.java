package de.pocketcloud.common.config.type;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class EnvironmentConfigType implements ConfigType {

    private static final Pattern PATTERN = Pattern.compile("^\\s*(?:export\\s+)?([A-Z0-9_\\-\\.]+)\\s*=\\s*(.*)\\s*$", Pattern.MULTILINE);

    @Override
    public Map<String, Object> decode(String content) {
        Map<String, Object> result = new LinkedHashMap<>();
        Matcher matcher = PATTERN.matcher(content);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2).trim();
            value = stripQuotes(value);
            result.put(key, value);
        }

        return result;
    }

    @Override
    public String encode(Map<String, Object> content) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Object> entry : content.entrySet()) {
            String key = String.valueOf(entry.getKey());
            String value = String.valueOf(entry.getValue());

            if (needsQuotes(value)) value = "\"" + value.replace("\"", "\\\"") + "\"";
            result.append(key).append("=").append(value).append("\n");
        }

        return result.toString();
    }

    private String stripQuotes(String value) {
        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }

    private boolean needsQuotes(String value) {
        return value.contains(" ") || value.contains("#") || value.contains("=") || value.isEmpty();
    }
}