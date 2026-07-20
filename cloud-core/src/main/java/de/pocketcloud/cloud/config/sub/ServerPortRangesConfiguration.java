package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.util.ServerPortRange;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class ServerPortRangesConfiguration extends OkaeriConfig implements ICloudConfig {

    private ServerPortRangeConfiguration server = new ServerPortRangeConfiguration();

    private ProxyPortRangeConfiguration proxy = new ProxyPortRangeConfiguration();

    @Override
    public void validate() {
        check(server, 40000, 65535);
        check(proxy, 19132, 20000);
    }

    private void check(PortRangeConfiguration config, int defaultStart, int defaultEnd) {
        int start = config.start();
        int end = config.end();

        String type = "server";
        if (config instanceof ProxyPortRangeConfiguration) type = "proxy";

        if (start <= 0 || end <= 0) {
            PocketCloud.instance().appendStartNotification("Invalid port range §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r: §bStart §7or §bend §7can not be less or equal to §b0§r: §cResetting the entry, please review your config...", CloudLogLevel.WARN, start, end, type);
            config.start(defaultStart);
            config.end(defaultEnd);
        } else if (start > end) {
            PocketCloud.instance().appendStartNotification("Invalid port range §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r: §bStart §ris §chigher §rthan §bend§r: §cResetting the entry, please review your config...", CloudLogLevel.WARN, start, end, type);
            config.start(defaultStart);
            config.end(defaultEnd);
        } else if ((start + 50) > end) {
            PocketCloud.instance().appendStartNotification("Invalid port range §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r: §bEnd §rneeds to be at least §b50 ports higher §rthan §bstart§r: §cResetting the entry, please review your config...", CloudLogLevel.WARN, start, end, type);
            config.start(defaultStart);
            config.end(defaultEnd);
        } else {
            PocketCloud.instance().appendStartNotification("Loaded server port range configuration §8(§b{}§8-§b{}§8) §rfor server type §8'§b{}§8'§r.", CloudLogLevel.SUCCESS, start, end, type);
        }
    }

    public ServerPortRange portRange(TemplateType type) {
        return switch (type) {
            case SERVER -> server.asPortRange();
            case PROXY -> proxy.asPortRange();
        };
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class ServerPortRangeConfiguration extends OkaeriConfig implements PortRangeConfiguration {

        private int start = 40000;

        private int end = 65535;

        @CustomKey("random-ports")
        @Comment({"Whether the port should be randomly generated between start and end"})
        private boolean randomPorts = true;

        public ServerPortRange asPortRange() {
            return new ServerPortRange(TemplateType.SERVER, start, end, randomPorts);
        }
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class ProxyPortRangeConfiguration extends OkaeriConfig implements PortRangeConfiguration {

        private int start = 19132;

        private int end = 20000;

        @CustomKey("random-ports")
        @Comment({"Whether the port should be randomly generated between start and end"})
        private boolean randomPorts = false;

        public ServerPortRange asPortRange() {
            return new ServerPortRange(TemplateType.PROXY, start, end, randomPorts);
        }
    }

    public interface PortRangeConfiguration {

        int start();

        int end();

        boolean randomPorts();

        PortRangeConfiguration start(int value);

        PortRangeConfiguration end(int value);

        PortRangeConfiguration randomPorts(boolean value);

        ServerPortRange asPortRange();
    }
}