package de.pocketcloud.cloud.language;

import de.pocketcloud.api.language.ILanguage;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.util.ArrayUtils;
import de.pocketcloud.common.util.FileUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Accessors(fluent = true)
public class Language implements ILanguage {

    private final Map<String, String> messages = new HashMap<>();

    private final String id;
    private final Map<String, String> defaultMessages;
    private final Path filePath;

    public Language(String id, Map<String, String> defaultMessages, Path filePath) {
        this.id = id;
        this.defaultMessages = defaultMessages;
        this.filePath = filePath;
    }

    @Override
    public void fetchAndRepair() {
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

            this.messages.putAll(realMessages);
        } else {
            CloudLogger.get().info("Language file §b{} §rnot found, generating...", filePath.toString());
            messages.clear();
            messages.putAll(defaultMessages);
            FileUtils.emitYamlFile(filePath, defaultMessages);
        }
    }

    @Override
    public String translate(String key, Object... args) {
        String message = messages.getOrDefault(key, key);
        message = message.replace("{PREFIX}", this.messages.getOrDefault("inGame.prefix", ""));
        for (int i = 0; i < args.length; i++) message = message.replace("{" + i + "}", args[i].toString());
        return message;
    }
}