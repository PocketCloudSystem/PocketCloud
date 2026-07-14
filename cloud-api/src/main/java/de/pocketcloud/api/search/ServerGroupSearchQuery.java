package de.pocketcloud.api.search;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.template.ITemplate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ServerGroupSearchQuery implements SearchQuery<IServerGroup> {

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
}