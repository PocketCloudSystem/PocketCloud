package de.pocketcloud.api.search;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.template.TemplateType;

public final class TemplateSearchQuery implements SearchQuery<ITemplate> {

    public static TemplateSearchQuery create() {
        return new TemplateSearchQuery();
    }

    private String namePrefix = null;
    private String serverGroupName = null;
    private TemplateType templateType = null;
    private String serverSoftwareName = null;

    public TemplateSearchQuery nameStartsWith(String prefix) {
        this.namePrefix = prefix;
        return this;
    }

    public TemplateSearchQuery ofServerGroup(IServerGroup serverGroup) {
        return ofServerGroup(serverGroup.name());
    }

    public TemplateSearchQuery ofServerGroup(String serverGroupName) {
        this.serverGroupName = serverGroupName;
        return this;
    }

    public TemplateSearchQuery ofTemplateType(TemplateType templateType) {
        this.templateType = templateType;
        return this;
    }

    public TemplateSearchQuery runningSoftware(String serverSoftwareName) {
        this.serverSoftwareName = serverSoftwareName;
        return this;
    }

    @Override
    public boolean matches(ITemplate t) {
        if (namePrefix != null && !t.name().startsWith(namePrefix)) return false;
        if (serverGroupName != null) {
            IServerGroup group = CloudAPI.instance().serverGroups().get(serverGroupName).orElse(null);
            if (group == null) return false;
            if (!group.templates().contains(t.name())) return false;
        }

        if (templateType != null && !t.templateType().equals(templateType)) return false;

        if (serverSoftwareName != null) {
            return t.serverSoftware().name().equals(serverSoftwareName);
        }

        return true;
    }
}