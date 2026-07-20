package de.pocketcloud.api.search;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.template.TemplateType;
import org.jetbrains.annotations.NotNull;

public final class PlayerSearchQuery implements ISearchQuery<ICloudPlayer> {

    public static PlayerSearchQuery create() {
        return new PlayerSearchQuery();
    }

    private String namePrefix = null;
    private String serverName = null;
    private String templateName = null;
    private String serverGroupName = null;
    private ServerStatus status = null;
    private VerificationStatus verificationStatus = null;
    private TemplateType templateType = null;
    private String serverSoftwareName = null;

    public PlayerSearchQuery nameStartsWith(String prefix) {
        this.namePrefix = prefix;
        return this;
    }

    public PlayerSearchQuery onServer(@NotNull ICloudServer server) {
        return onServer(server.name());
    }

    public PlayerSearchQuery onServer(@NotNull String serverName) {
        this.serverName = serverName;
        return this;
    }

    public PlayerSearchQuery ofTemplate(@NotNull ITemplate template) {
        return ofTemplate(template.name());
    }

    public PlayerSearchQuery ofTemplate(@NotNull String templateName) {
        this.templateName = templateName;
        return this;
    }

    public PlayerSearchQuery inGroup(@NotNull IServerGroup serverGroup) {
        return inGroup(serverGroup.name());
    }

    public PlayerSearchQuery inGroup(@NotNull String serverGroupName) {
        this.serverGroupName = serverGroupName;
        return this;
    }

    public PlayerSearchQuery withStatus(@NotNull ServerStatus status) {
        this.status = status;
        return this;
    }

    public PlayerSearchQuery withVerification(@NotNull VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
        return this;
    }

    public PlayerSearchQuery ofType(@NotNull TemplateType templateType) {
        this.templateType = templateType;
        return this;
    }

    public PlayerSearchQuery runningSoftware(@NotNull String serverSoftwareName) {
        this.serverSoftwareName = serverSoftwareName;
        return this;
    }

    @Override
    public boolean matches(ICloudPlayer p) {
        if (namePrefix != null && !p.name().startsWith(namePrefix)) return false;

        var server = p.currentServer().orElse(null);
        var proxy = p.currentProxy().orElse(null);

        if (serverName != null) {
            boolean match = (server != null && server.name().equals(serverName)) ||
                    (proxy != null && proxy.name().equals(serverName));
            if (!match) return false;
        }

        if (templateName != null) {
            boolean match = (server != null && server.templateName().equals(templateName)) ||
                    (proxy != null && proxy.templateName().equals(templateName));
            if (!match) return false;
        }

        if (serverGroupName != null) {
            IServerGroup group = CloudAPI.instance().serverGroups().get(serverGroupName).orElse(null);
            if (group == null) return false;

            boolean match = (server != null && group.templates().contains(server.templateName())) ||
                    (proxy != null && group.templates().contains(proxy.templateName()));
            if (!match) return false;
        }

        if (status != null) {
            boolean match = (server != null && server.status().equals(status)) ||
                    (proxy != null && proxy.status().equals(status));
            if (!match) return false;
        }

        if (verificationStatus != null) {
            boolean match = (server != null && server.verificationStatus().equals(verificationStatus)) ||
                    (proxy != null && proxy.verificationStatus().equals(verificationStatus));
            if (!match) return false;
        }

        if (templateType != null) {
            boolean match = (server != null && server.template().templateType().equals(templateType)) ||
                    (proxy != null && proxy.template().templateType().equals(templateType));
            if (!match) return false;
        }

        if (serverSoftwareName != null) {
            return (server != null && server.template().serverSoftware().name().equals(serverSoftwareName)) ||
                    (proxy != null && proxy.template().serverSoftware().name().equals(serverSoftwareName));
        }

        return true;
    }
}