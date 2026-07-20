package de.pocketcloud.api.provider;

import de.pocketcloud.api.component.builder.ITemplateBuilder;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.TemplateSearchQuery;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public interface ITemplateProvider {

    ITemplateBuilder builder();

    boolean check(String name);

    /**
     * This method returns null on the cloud-side.
     */
    ITemplate current();

    Optional<ITemplate> get(String name);

    Collection<ITemplate> query(TemplateSearchQuery searchQuery);

    Collection<ITemplate> query(Consumer<TemplateSearchQuery> queryConsumer);

    Collection<ITemplate> getAll();
}