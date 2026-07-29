package de.pocketcloud.api.provider;

import de.pocketcloud.api.language.ILanguage;

import java.util.Collection;
import java.util.Optional;

public interface ILanguageProvider {

    void register(ILanguage language);

    void unregister(ILanguage language);

    ILanguage current();

    ILanguage fallback();

    Optional<ILanguage> get(String name);

    Collection<ILanguage> getAll();
}