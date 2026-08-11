package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.language.ILanguage;
import de.pocketcloud.api.provider.ILanguageProvider;
import de.pocketcloud.bridge.component.MinimalLanguage;
import lombok.Setter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class LanguageProvider implements ILanguageProvider {

    /**
     * Will be set via SyncPacket
     */
    @Setter
    private MinimalLanguage currentLanguage = null;

    @Override
    public void register(ILanguage language) {
        throw new UnsupportedOperationException("You cannot register languages on cloud servers");
    }

    @Override
    public void unregister(ILanguage language) {
        throw new UnsupportedOperationException("You cannot unregister languages on cloud servers");
    }

    @Override
    public ILanguage current() {
        return currentLanguage;
    }

    @Override
    public ILanguage fallback() {
        return currentLanguage;
    }

    @Override
    public Optional<ILanguage> get(String name) {
        return Optional.empty();
    }

    @Override
    public int languageCount() {
        return 1;
    }

    @Override
    public Collection<ILanguage> getAll() {
        return List.of(current());
    }
}