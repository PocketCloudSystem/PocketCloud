package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.component.builder.ITemplateBuilder;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteTemplateProvider;
import de.pocketcloud.api.search.TemplateSearchQuery;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.component.Template;
import de.pocketcloud.bridge.component.builder.TemplateBuilder;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class TemplateProvider implements IWriteTemplateProvider {

    private final Map<String, ITemplate> templates = new ConcurrentHashMap<>();

    @Override
    public ITemplateBuilder builder() {
        return new TemplateBuilder();
    }

    @Override
    public void create(ITemplateBuilder builder) {
        throw new NotImplementedException("You cannot create templates on cloud servers");
    }

    @Override
    public void delete(ITemplate template) {
        throw new NotImplementedException("You cannot delete templates on cloud servers");
    }

    @Override
    public void add(ITemplate template) {
        if (templates.containsKey(template.name())) {
            ((Template) templates.get(template.name())).syncIn(template);
        } else templates.put(template.name(), template);
    }

    @Override
    public void edit(ITemplate template, TemplateEditData newData) {
        throw new NotImplementedException("You cannot edit templates on cloud servers.");
    }

    @Override
    public void remove(ITemplate template) {
        templates.remove(template.name());
    }

    @Override
    public boolean check(String name) {
        return templates.containsKey(name);
    }

    @Override
    public ITemplate current() {
        return Optional.ofNullable(templates.get(CloudBridge.instance().environmentConfig().localTemplateName())).orElseThrow(() -> new IllegalStateException("Current template should not be null, called too early?"));
    }

    @Override
    public Optional<ITemplate> get(String name) {
        return Optional.ofNullable(templates.get(name));
    }

    @Override
    public Collection<ITemplate> query(TemplateSearchQuery searchQuery) {
        return templates.values().stream()
                .filter(searchQuery::matches)
                .toList();
    }

    @Override
    public Collection<ITemplate> query(Consumer<TemplateSearchQuery> queryConsumer) {
        TemplateSearchQuery searchQuery = new TemplateSearchQuery();
        queryConsumer.accept(searchQuery);
        return query(searchQuery);
    }

    @Override
    public Collection<ITemplate> getAll() {
        return templates.values().stream().toList();
    }
}