package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.server.CloudServer;
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

    public PacketExcluder client(ServerClient client) {
        predicate = predicate.or(target -> target instanceof ServerClient c && c.equals(client));
        return this;
    }

    public PacketExcluder server(ICloudServer server) {
        predicate = predicate.or(target -> target instanceof CloudServer s && s.equals(server));
        return this;
    }

    public PacketExcluder template(ITemplate template) {
        return templateName(template.name());
    }

    public PacketExcluder templateName(String templateName) {
        predicate = predicate.or(target -> target instanceof CloudServer s && s.templateName().equals(templateName));
        return this;
    }

    public PacketExcluder templateType(TemplateType type) {
        predicate = predicate.or(target -> target instanceof CloudServer s && s.template().templateType() == type);
        return this;
    }

    public PacketExcluder serverGroup(IServerGroup group) {
        return serverGroupName(group.name());
    }

    public PacketExcluder serverGroupName(String serverGroupName) {
        predicate = predicate.or(target -> target instanceof CloudServer s && s.template().isParentGroup(serverGroupName));
        return this;
    }

    public PacketExcluder status(ServerStatus status) {
        predicate = predicate.or(target -> target instanceof CloudServer s && s.status().equals(status));
        return this;
    }

    public PacketExcluder verificationStatus(VerificationStatus verificationStatus) {
        predicate = predicate.or(target -> target instanceof CloudServer s && s.verificationStatus().equals(verificationStatus));
        return this;
    }

    public PacketExcluder namePrefix(String prefix) {
        predicate = predicate.or(target -> target instanceof CloudServer s && s.name().startsWith(prefix));
        return this;
    }

    public boolean shouldExclude(ServerClient client) {
        CloudServer server = client.server();
        Channel channel = client.channel();
        return predicate.test(channel) || predicate.test(server);
    }
}