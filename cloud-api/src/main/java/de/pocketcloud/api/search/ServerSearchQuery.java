package de.pocketcloud.api.search;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;

import java.util.Map;

public final class ServerSearchQuery implements ISearchQuery<ICloudServer>, Writable<Map<String, Object>> {

    public static ServerSearchQuery create() {
        return new ServerSearchQuery();
    }

    private String namePrefix = null;
    private String templateName = null;
    private String serverGroupName = null;
    private Boolean lobby = null;
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

    public ServerSearchQuery lobby(boolean lobby) {
        this.lobby = lobby;
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

        if (lobby != null && !s.template().settings().lobby()) return false;

        if (status != null && !s.status().equals(status)) return false;
        if (verificationStatus != null && !s.verificationStatus().equals(verificationStatus)) return false;

        if (templateType != null && !s.template().templateType().equals(templateType)) return false;

        if (serverSoftwareName != null) {
            return s.template().serverSoftware().name().equals(serverSoftwareName);
        }

        return true;
    }

    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static ServerSearchQuery read(Map<String, Object> data) {
        ServerSearchQuery query = new ServerSearchQuery();
        query.namePrefix = (String) data.get("namePrefix");
        query.templateName = (String) data.get("templateName");
        query.serverGroupName = (String) data.get("serverGroupName");
        query.lobby = (Boolean) data.get("lobby");

        try {
            query.status = data.get("status") != null ? ServerStatus.valueOf(data.get("status").toString().toUpperCase()) : null;
        } catch (IllegalArgumentException _) {
            query.status = null;
        }

        try {
            query.verificationStatus = data.get("verificationStatus") != null ? VerificationStatus.valueOf(data.get("status").toString()) : null;
        } catch (IllegalArgumentException _) {
            query.verificationStatus = null;
        }

        try {
            query.templateType = data.get("templateType") != null ? TemplateType.valueOf(data.get("templateType").toString()) : null;
        } catch (IllegalArgumentException _) {
            query.templateType = null;
        }

        query.serverSoftwareName = (String) data.get("serverSoftwareName");

        return query;
    }
}