package de.pocketcloud.api.network.packet;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.network.client.IServerClient;
import de.pocketcloud.api.search.ServerGroupSearchQuery;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.template.TemplateType;
import io.netty.channel.Channel;

import java.util.function.Predicate;

public final class PacketExcluder {

    private Predicate<Object> predicate = _ -> false;

    public static PacketExcluder create() {
        return new PacketExcluder();
    }

    public PacketExcluder channel(Channel channel) {
        predicate = predicate.or(target -> target instanceof Channel c && c.equals(channel));
        return this;
    }

    public PacketExcluder client(IServerClient client) {
        predicate = predicate.or(target -> target instanceof IServerClient c && c.equals(client));
        return this;
    }

    public PacketExcluder server(ICloudServer server) {
        predicate = predicate.or(target -> target instanceof ICloudServer s && s.equals(server));
        return this;
    }

    public PacketExcluder template(ITemplate template) {
        return templateName(template.name());
    }

    public PacketExcluder templateName(String templateName) {
        predicate = predicate.or(target -> target instanceof ICloudServer s && s.templateName().equals(templateName));
        return this;
    }

    public PacketExcluder templateType(TemplateType type) {
        predicate = predicate.or(target -> target instanceof ICloudServer s && s.template().templateType() == type);
        return this;
    }

    public PacketExcluder serverGroup(IServerGroup group) {
        return serverGroupName(group.name());
    }

    public PacketExcluder serverGroupName(String serverGroupName) {
        predicate = predicate.or(target -> target instanceof ICloudServer s && !CloudAPI.instance().serverGroups().query(ServerGroupSearchQuery.create()
                .nameStartsWith(serverGroupName)
                .withTemplates(s.templateName())).isEmpty());
        return this;
    }

    public PacketExcluder status(ServerStatus status) {
        predicate = predicate.or(target -> target instanceof ICloudServer s && s.status().equals(status));
        return this;
    }

    public PacketExcluder verificationStatus(VerificationStatus verificationStatus) {
        predicate = predicate.or(target -> target instanceof ICloudServer s && s.verificationStatus().equals(verificationStatus));
        return this;
    }

    public PacketExcluder namePrefix(String prefix) {
        predicate = predicate.or(target -> target instanceof ICloudServer s && s.name().startsWith(prefix));
        return this;
    }

    public boolean shouldExclude(IServerClient client) {
        ICloudServer server = client.server();
        Channel channel = client.channel();
        return predicate.test(channel) || predicate.test(server);
    }
}