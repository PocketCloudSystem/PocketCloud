package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Exclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class ServerAddressesConfiguration extends OkaeriConfig implements ICloudConfig {

    @Exclude
    public static final String DEFAULT_PUBLIC_ADDRESS = "127.0.0.1";
    @Exclude
    public static final String DEFAULT_SERVER_BIND_ADDRESS = "127.0.0.1";
    @Exclude
    public static final String DEFAULT_PROXY_BIND_ADDRESS = "0.0.0.0";

    @CustomKey("public-address")
    @Comment({
            "Address under which proxies and other servers reach the servers started by this cloud.",
            "This is the address sent to proxies when a server is registered.",
            "Keep 127.0.0.1 if everything runs on one machine, otherwise use the IP of this machine."
    })
    private String publicAddress = DEFAULT_PUBLIC_ADDRESS;

    @CustomKey("server-bind-address")
    @Comment({
            "Address the sub-servers (e.g. PowerNukkitX) bind to.",
            "Use 0.0.0.0 or the IP of this machine if proxies on other machines need to reach them."
    })
    private String serverBindAddress = DEFAULT_SERVER_BIND_ADDRESS;

    @CustomKey("proxy-bind-address")
    @Comment({"Address the proxies (e.g. WaterdogPE) bind to."})
    private String proxyBindAddress = DEFAULT_PROXY_BIND_ADDRESS;

    @Override
    public void validate() {
        if (isBlank(publicAddress)) {
            publicAddress = reset("public-address", DEFAULT_PUBLIC_ADDRESS);
        }

        if (isBlank(serverBindAddress)) {
            serverBindAddress = reset("server-bind-address", DEFAULT_SERVER_BIND_ADDRESS);
        }

        if (isBlank(proxyBindAddress)) {
            proxyBindAddress = reset("proxy-bind-address", DEFAULT_PROXY_BIND_ADDRESS);
        }

        publicAddress = publicAddress.trim();
        serverBindAddress = serverBindAddress.trim();
        proxyBindAddress = proxyBindAddress.trim();
    }

    public String bindAddress(TemplateType type) {
        return type.isProxy() ? proxyBindAddress : serverBindAddress;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String reset(String key, String defaultValue) {
        PocketCloud.instance().appendStartNotification("Invalid §b{} §rgiven in server settings, reset to default... §8(§b{}§8)", CloudLogLevel.WARN, key, defaultValue);
        return defaultValue;
    }
}
