package de.pocketcloud.cloud.template.util;

import de.pocketcloud.api.template.settings.TemplateSettings;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TemplateHelper {

    public static final List<String> KEYS = List.of(
            "name", "lobby", "maintenance", "staticServers", "alwaysCopyToStaticServers",
            "maxPlayerCount", "minServerCount", "maxServerCount", "startNewPercentage",
            "autoStart", "templateType", "serverSoftware"
    );

    public static final List<String> EDITABLE_KEYS = List.of(
            "lobby", "maintenance", "staticServers", "alwaysCopyToStaticServers",
            "maxPlayerCount", "minServerCount", "maxServerCount",
            "startNewPercentage", "autoStart"
    );

    public static final List<String> NECESSARY_KEYS = List.of("name", "lobby", "type", "software");

    public static final List<String> UNNECESSARY_KEYS = List.of(
            "maintenance", "staticServers", "alwaysCopyToStaticServers",
            "maxPlayerCount", "minServerCount", "maxServerCount",
            "startNewPercentage", "autoStart"
    );

    public static final Map<String, Object> DEFAULT_VALUES = new HashMap<>();
    public static final Map<String, Class<?>> KEY_TYPES = new HashMap<>();

    static {
        DEFAULT_VALUES.put("lobby", false);
        DEFAULT_VALUES.put("maintenance", true);
        DEFAULT_VALUES.put("staticServers", false);
        DEFAULT_VALUES.put("alwaysCopyToStaticServers", false);
        DEFAULT_VALUES.put("maxPlayerCount", 20);
        DEFAULT_VALUES.put("minServerCount", 0);
        DEFAULT_VALUES.put("maxServerCount", 2);
        DEFAULT_VALUES.put("startNewPercentage", 100.0f);
        DEFAULT_VALUES.put("autoStart", true);

        KEY_TYPES.put("lobby", Boolean.class);
        KEY_TYPES.put("maintenance", Boolean.class);
        KEY_TYPES.put("staticServers", Boolean.class);
        KEY_TYPES.put("alwaysCopyToStaticServers", Boolean.class);
        KEY_TYPES.put("maxPlayerCount", Integer.class);
        KEY_TYPES.put("minServerCount", Integer.class);
        KEY_TYPES.put("maxServerCount", Integer.class);
        KEY_TYPES.put("startNewPercentage", Double.class);
        KEY_TYPES.put("autoStart", Boolean.class);
        KEY_TYPES.put("templateType", String.class);
        KEY_TYPES.put("serverSoftware", String.class);
    }

    private static final Set<String> EDITABLE_KEYS_SET = Set.copyOf(EDITABLE_KEYS);

    private static final Map<String, String> CONVERSION = Map.of(
            "staticservers", "staticServers",
            "alwayscopytostaticservers", "alwaysCopyToStaticServers",
            "maxplayercount", "maxPlayerCount",
            "minservercount", "minServerCount",
            "maxservercount", "maxServerCount",
            "startnewpercentage", "startNewPercentage",
            "autostart", "autoStart",
            "templatetype", "templateType",
            "serversoftware", "serverSoftware"
    );

    public static void fillKeys(Map<String, Object> data) {
        for (String key : UNNECESSARY_KEYS) {
            data.putIfAbsent(key, DEFAULT_VALUES.get(key));
        }
    }

    @Nullable
    public static TemplateSettings sumSettingsToInstance(Map<String, Object> data) {
        if (!data.keySet().containsAll(EDITABLE_KEYS)) return null;

        Map<String, Object> onlySettings = new HashMap<>(EDITABLE_KEYS.size());
        for (String key : EDITABLE_KEYS) onlySettings.put(key, data.get(key));
        return TemplateSettings.read(onlySettings);
    }

    public static boolean checkValue(String value, String key, ValidationResult result) {
        Class<?> type = KEY_TYPES.get(key);
        if (type == Boolean.class) {
            result.expected = "true | false";
            if (value.equals("true") || value.equals("false")) {
                result.realValue = Boolean.parseBoolean(value);
                return true;
            }
        } else if (type == Integer.class || type == Double.class) {
            result.expected = "number";
            if (isNumeric(value)) {
                result.realValue = parseNumeric(type, value);
                return true;
            }
        }
        return false;
    }

    public static boolean checkRawValue(Object value, String key, ValidationResult result) {
        Class<?> type = KEY_TYPES.get(key);
        if (type == Boolean.class) {
            result.expected = "boolean";
            if (value instanceof Boolean) {
                result.realValue = value;
                return true;
            }
        } else if (type == Integer.class || type == Double.class) {
            result.expected = "number";
            if (value instanceof Number) {
                result.realValue = parseNumeric(type, value.toString());
                return true;
            }
        }
        return false;
    }

    public static boolean checkKey(String key) {
        return EDITABLE_KEYS_SET.contains(key);
    }

    public static String convert(String key) {
        return CONVERSION.getOrDefault(key.toLowerCase(), key);
    }

    private static boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static Number parseNumeric(Class<?> type, String value) {
        if (type == Float.class) return Math.max(0f, Float.parseFloat(value));
        return Math.max(0, (int) Double.parseDouble(value));
    }

    public static final class ValidationResult {

        public String expected;
        public Object realValue;
    }
}