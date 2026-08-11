package de.pocketcloud.cloud.language;

import de.pocketcloud.api.language.DefaultMessages;
import de.pocketcloud.api.language.ILanguage;
import de.pocketcloud.api.provider.ILanguageProvider;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.lifecycle.Loadable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class LanguageManager implements Loadable, ILanguageProvider {

    private final ILanguage FALLBACK = new Language(DefaultLanguages.ENGLISH, DefaultMessages.MESSAGES_EN, PocketCloudPaths.storage().inGame().with(DefaultLanguages.ENGLISH + ".yml").asPath());
    private final Map<String, ILanguage> languages = new HashMap<>();
    private ILanguage current = null;

    @Override
    public void load() {
        register(FALLBACK);
        register(new Language(DefaultLanguages.GERMAN, DefaultMessages.MESSAGES_GER, PocketCloudPaths.storage().inGame().with(DefaultLanguages.GERMAN + ".yml").asPath()));

        String current = PocketCloud.instance().config().language();
        if (!languages.containsKey(current)) {
            this.current = new Language(current, DefaultMessages.MESSAGES_EN, PocketCloudPaths.storage().inGame().with(current + ".yml").asPath());
            register(this.current);
        } else {
            this.current = languages.get(current);
        }
    }

    @Override
    public void unload() {
        languages.clear();
    }

    @Override
    public void register(ILanguage language) {
        languages.put(language.id(), language);
        language.fetchAndRepair();
    }

    @Override
    public void unregister(ILanguage language) {
        languages.remove(language.id());
    }

    @Override
    public ILanguage current() {
        if (current == null) return fallback();
        return current;
    }

    @Override
    public ILanguage fallback() {
        return FALLBACK;
    }

    @Override
    public Optional<ILanguage> get(String name) {
        return Optional.ofNullable(languages.get(name));
    }

    @Override
    public int languageCount() {
        return languages.size();
    }

    @Override
    public Collection<ILanguage> getAll() {
        return languages.values().stream().toList();
    }
}