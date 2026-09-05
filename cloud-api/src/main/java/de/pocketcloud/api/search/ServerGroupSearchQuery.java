package de.pocketcloud.api.search;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ServerGroupSearchQuery implements ISearchQuery<IServerGroup>, Writable<Map<String, Object>> {

    public static ServerGroupSearchQuery create() {
        return new ServerGroupSearchQuery();
    }

    private String namePrefix = null;
    private final Set<String> templateNames = new HashSet<>();

    public ServerGroupSearchQuery nameStartsWith(String prefix) {
        this.namePrefix = prefix;
        return this;
    }

    public ServerGroupSearchQuery withTemplates(String... templateNames) {
        this.templateNames.addAll(Arrays.asList(templateNames));
        return this;
    }

    public ServerGroupSearchQuery withTemplates(ITemplate... templates) {
        Arrays.stream(templates).map(ITemplate::name).forEach(this.templateNames::add);
        return this;
    }

    public ServerGroupSearchQuery includingTemplate(String templateName) {
        this.templateNames.add(templateName);
        return this;
    }

    public ServerGroupSearchQuery includingTemplate(ITemplate template) {
        return includingTemplate(template.name());
    }

    public ServerGroupSearchQuery clearTemplates() {
        this.templateNames.clear();
        return this;
    }

    @Override
    public boolean matches(IServerGroup sG) {
        if (namePrefix != null && !sG.name().startsWith(namePrefix)) return false;
        if (templateNames.isEmpty()) return true;

        return new HashSet<>(sG.templates()).containsAll(templateNames);
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static ServerGroupSearchQuery read(Map<String, Object> data) {
        ServerGroupSearchQuery query = new ServerGroupSearchQuery();

        query.namePrefix = (String) data.get("namePrefix");

        Object templates = data.get("templateNames");
        if (templates instanceof Iterable<?> iterable) {
            for (Object template : iterable) {
                if (template != null) {
                    query.templateNames.add(template.toString());
                }
            }
        }

        return query;
    }
}