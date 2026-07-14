package de.pocketcloud.api.provider;

import de.pocketcloud.api.language.ILanguage;

import java.util.Optional;

public interface ILanguageProvider<T extends ILanguage> {

    void register(T language);

    void unregister(T language);

    T current();

    T fallback();

    Optional<T> get(String name);
}