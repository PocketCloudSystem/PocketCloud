package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings of this node ({@code storage/configs/node.yml}).
 * <p>
 * Without a cluster certificate (see {@code node add} / {@code --join}) the node runs alone
 * and is always head, which is the classic single-server mode.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class NodeConfig extends OkaeriConfig implements ICloudConfig {

    @Comment({"Unique name of this node in the cluster. Only letters, digits, '-' and '_'."})
    private String name = "node-1";

    @Comment({"Address other nodes use to reach this node (cluster port and game servers)."})
    private String advertisedAddress = "127.0.0.1";

    @Comment({"Address the cluster port binds to."})
    private String clusterBindAddress = "0.0.0.0";

    @Comment({"Port for node-to-node connections (TLS)."})
    private int clusterPort = 3657;

    @Comment({"Memory in MB this node may use for servers. 0 = no limit."})
    private int maxMemoryMb = 0;

    @Comment({"Nodes to contact when joining (host:port). Filled automatically by --join."})
    private List<String> seeds = new ArrayList<>();

    @Override
    public void validate() {
        if (name == null || !name.matches("[a-zA-Z0-9_\\-]{1,32}")) {
            PocketCloud.instance().appendStartNotification("Invalid node name in node.yml, reset to default... §8(§bnode-1§8)", CloudLogLevel.WARN);
            name = "node-1";
        }

        if (clusterPort < 1 || clusterPort > 65535) {
            PocketCloud.instance().appendStartNotification("Invalid cluster port in node.yml, reset to default... §8(§b3657§8)", CloudLogLevel.WARN);
            clusterPort = 3657;
        }

        if (maxMemoryMb < 0) maxMemoryMb = 0;
        if (seeds == null) seeds = new ArrayList<>();
    }
}
