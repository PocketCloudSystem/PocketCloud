package de.pocketcloud.api.language;

import java.nio.file.Path;
import java.util.Map;

public interface ILanguage {

    /**
     * This method should automatically fetch the messages and add missing language keys.
     */
    void fetchAndRepair();

    String translate(String key, Object... args);

    String translate(String key, Map<String, Object> args);

    default String translate(LanguageKey key, Object... args) {
        return translate(key.langKey(), args);
    }

    default String translate(LanguageKey key, Map<String, Object> args) {
        return translate(key.langKey(), args);
    }

    String id();

    Map<String, String> messages();

    Map<String, String> defaultMessages();

    Path filePath();
}