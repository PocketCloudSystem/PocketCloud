package de.pocketcloud.cloud.language;

import de.pocketcloud.cloud.config.impl.MainConfig;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.ArrayUtils;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Accessors(fluent = true)
public enum Language {

    ENGLISH(PocketCloudPaths.storage().inGame().with("de_DE.yml").asPath(), new String[]{"de_DE", "ger", "Deutsch"}, DefaultMessages.MESSAGES),
    GERMAN(PocketCloudPaths.storage().inGame().with("en_US.yml").asPath(), new String[]{"en_US", "en", "English"}, DefaultMessages.MESSAGES_DE);

    private final Map<String, String> messages;

    private final Path filePath;
    private final String[] aliases;
    private final Map<String, String> defaultMessages;

    Language(Path filePath, String[] aliases, Map<String, String> defaultMessages) {
        this.filePath = filePath;
        this.aliases = aliases;
        this.defaultMessages = defaultMessages;

        if (Files.isRegularFile(filePath)) {
            Map<String, Object> messages = FileUtils.parseYamlFile(filePath);
            Map<String, String> realMessages = new HashMap<>();
            for (Map.Entry<String, Object> entry : messages.entrySet()) {
                if (entry.getValue() instanceof String) {
                    realMessages.put(entry.getKey(), (String) entry.getValue());
                }
            }

            AtomicInteger affected = new AtomicInteger();
            ArrayUtils.fillMissingKeys(realMessages, defaultMessages, affected, true);
            if (affected.get() > 0) {
                CloudLogger.get().info("Incomplete language file §b{}§r, completing the file with the missing language keys...", filePath.toString());
                FileUtils.emitYamlFile(filePath, realMessages);
            }

            this.messages = realMessages;
        } else {
            CloudLogger.get().info("Language file §b{} §rnot found, generating...", filePath.toString());
            this.messages = defaultMessages;
            FileUtils.emitYamlFile(filePath, defaultMessages);
        }
    }

    public String translate(String message, Object ... args) {
        message = message.replace("{PREFIX}", this.messages.getOrDefault("inGame.prefix", ""));
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i].toString());
        }
        return message;
    }

    public static Language current() {
        try {
            return Language.valueOf(MainConfig.instance().getLanguage());
        } catch (IllegalArgumentException e) {
            return fallback();
        }
    }

    public static Language fallback() {
        return Language.ENGLISH;
    }

    @Nullable
    public static Language get(String name) {
        return Arrays.stream(values()).filter(l -> l.name().equalsIgnoreCase(name) || Arrays.stream(l.aliases()).anyMatch(a -> a.equalsIgnoreCase(name)))
                .findFirst()
                .orElse(null);
    }
}