package de.pocketcloud.cloud.config.type;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PropertiesConfigType implements ConfigType {

    @Override
    public Map<String, Object> decode(String content) {
        Map<String, Object> result = new LinkedHashMap<>();

        Pattern pattern = Pattern.compile("^\\s*([a-zA-Z0-9_\\-\\.]+)[ \\t]*=([^\\r\\n]*)", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String key = matcher.group(1);
            String rawValue = matcher.group(2).trim();
            result.put(key, parseValue(rawValue));
        }

        return result;
    }

    @Override
    public String encode(Map<String, Object> content) {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<String, Object> entry : content.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (value instanceof Boolean) value = (Boolean) value ? "on" : "off";
            result.append(key).append("=").append(value).append("\r\n");
        }

        return result.toString();
    }

    private Object parseValue(String v) {
        String lower = v.toLowerCase();
        if (lower.equals("on") || lower.equals("true") || lower.equals("yes")) return true;
        if (lower.equals("off") || lower.equals("false") || lower.equals("no")) return false;

        try {
            if (v.equals(String.valueOf(Integer.parseInt(v)))) {
                return Integer.parseInt(v);
            }
        } catch (NumberFormatException _) {}

        try {
            if (v.equals(String.valueOf(Double.parseDouble(v)))) {
                return Double.parseDouble(v);
            }
        } catch (NumberFormatException _) {}

        return v;
    }
}