package de.pocketcloud.api.search;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.template.TemplateType;

public final class ServerSearchQuery implements SearchQuery<ICloudServer> {

    public static ServerSearchQuery create() {
        return new ServerSearchQuery();
    }

    private String namePrefix = null;
    private String templateName = null;
    private String serverGroupName = null;
    private ServerStatus status = null;
    private VerificationStatus verificationStatus = null;
    private TemplateType templateType = null;
    private String serverSoftwareName = null;

    public ServerSearchQuery nameStartsWith(String prefix) {
        this.namePrefix = prefix;
        return this;
    }

    public ServerSearchQuery ofTemplate(ITemplate template) {
        return ofTemplate(template.name());
    }

    public ServerSearchQuery ofTemplate(String templateName) {
        this.templateName = templateName;
        return this;
    }

    public ServerSearchQuery inGroup(IServerGroup serverGroup) {
        return inGroup(serverGroup.name());
    }

    public ServerSearchQuery inGroup(String serverGroupName) {
        this.serverGroupName = serverGroupName;
        return this;
    }

    public ServerSearchQuery withStatus(ServerStatus status) {
        this.status = status;
        return this;
    }

    public ServerSearchQuery withVerification(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
        return this;
    }

    public ServerSearchQuery ofType(TemplateType templateType) {
        this.templateType = templateType;
        return this;
    }

    public ServerSearchQuery runningSoftware(String serverSoftwareName) {
        this.serverSoftwareName = serverSoftwareName;
        return this;
    }

    @Override
    public boolean matches(ICloudServer s) {
        if (namePrefix != null && !s.name().startsWith(namePrefix)) return false;
        if (templateName != null && !s.templateName().equals(templateName)) return false;
        if (serverGroupName != null) {
            IServerGroup group = CloudAPI.instance().serverGroups().get(serverGroupName).orElse(null);
            if (group == null) return false;
            if (!group.templates().contains(s.templateName())) return false;
        }

        if (status != null && !s.status().equals(status)) return false;
        if (verificationStatus != null && !s.verificationStatus().equals(verificationStatus)) return false;

        if (templateType != null && !s.template().templateType().equals(templateType)) return false;

        if (serverSoftwareName != null) {
            return s.template().serverSoftware().name().equals(serverSoftwareName);
        }

        return true;
    }
}