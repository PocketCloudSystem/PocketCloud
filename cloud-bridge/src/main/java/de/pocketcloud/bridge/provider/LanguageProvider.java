package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.language.ILanguage;
import de.pocketcloud.api.provider.ILanguageProvider;
import de.pocketcloud.bridge.component.MinimalLanguage;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Optional;

public final class LanguageProvider implements ILanguageProvider {

    /**
     * Will be set via SyncPacket
     */
    @Setter
    private MinimalLanguage currentLanguage = null;

    @Override
    public void register(ILanguage language) {
        throw new NotImplementedException("You cannot register languages on cloud servers");
    }

    @Override
    public void unregister(ILanguage language) {
        throw new NotImplementedException("You cannot unregister languages on cloud servers");
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
}