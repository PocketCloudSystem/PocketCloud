package de.pocketcloud.api.provider;

import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.search.SearchQuery;
import de.pocketcloud.api.template.util.TemplateEditData;

import java.util.Collection;
import java.util.Optional;

public interface ITemplateProvider<T extends ITemplate> {

    void add(T template);

    void edit(T template, TemplateEditData newData);

    void remove(T template);

    boolean check(String name);

    Optional<T> get(String name);

    Collection<T> query(SearchQuery<? extends ITemplate> searchQuery);

    Collection<T> getAll();
}