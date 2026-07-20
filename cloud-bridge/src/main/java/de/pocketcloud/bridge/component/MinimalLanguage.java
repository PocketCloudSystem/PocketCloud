package de.pocketcloud.bridge.component;

import de.pocketcloud.api.language.ILanguage;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.NotImplementedException;

import java.nio.file.Path;
import java.util.Map;

@Accessors(fluent = true)
public record MinimalLanguage(String id, Map<String, String> messages) implements ILanguage {

    @Override
    public void fetchAndRepair() {
        throw new NotImplementedException();
    }

    @Override
    public String translate(String key, Object... args) {
        String message = messages.getOrDefault(key, key);
        message = message.replace("{PREFIX}", this.messages.getOrDefault("inGame.prefix", ""));
        for (int i = 0; i < args.length; i++) message = message.replace("{" + i + "}", args[i].toString());
        return message;
    }

    @Override
    public Map<String, String> defaultMessages() {
        throw new NotImplementedException();
    }

    @Override
    public Path filePath() {
        throw new NotImplementedException();
    }
}