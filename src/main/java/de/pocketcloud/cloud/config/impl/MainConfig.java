package de.pocketcloud.cloud.config.impl;

import de.pocketcloud.cloud.provider.database.MySqlSettings;
import de.pocketcloud.cloud.util.StringUtils;
import de.pocketcloud.configlib.*;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Accessors(fluent = true)
public final class MainConfig extends Configuration {

    @Getter
    @Accessors(fluent = false)
    @Ignored
    private static MainConfig instance = null;

    @Comment({"The name of the cloud."})
    private String cloudName = "main-cloud";
    @Comment({"The language used in-game."})
    private String language = "en_US";
    @Comment({"The data provider used for storing data."})
    private String provider = "json";
    @Comment({"The network configuration for the cloud."})
    private ConfigMap network = new ConfigMap()
            .set("address", "127.0.0.1", "Bind address for the socket")
            .set("port", 3656, "Port for the socket")
            .set("encryption", false, "Encryption for the socket traffic")
            .set("packet_size_limit", 10_000_000, "Traffic limit in bytes");
    @Comment({"The HTTP service configuration for the cloud."})
    private ConfigMap httpServer = new ConfigMap()
            .set("address", "0.0.0.0", "Bind address for the HTTP server")
            .set("port", 8080, "Port for the HTTP server")
            .set("auth-key", StringUtils.generate(20), "Authorization key for incoming HTTP requests");
    @Comment({"The MySQL configuration for the cloud. Only used if provider is set to 'mysql'"})
    private ConfigMap mysqlSettings = new ConfigMap()
            .set("address", "127.0.0.1", "MySQL server address")
            .set("port", 3306, "MySQL server port")
            .set("database", "cloud", "Used MySQL database")
            .set("user", "user", "Used MySQL username")
            .set("password", "123", "Used MySQL user password");

    public MainConfig() {
        super("storage/configs/config.yml", ConfigType.YAML);
        instance = this;
        reload();
    }

    public void reload() {
        var changes = new AtomicInteger(0);
        if (!load(changes) || changes.get() > 0) save();
    }

    public MainConfig setCloudName(String cloudName) {
        this.cloudName = cloudName;
        return this;
    }

    public MainConfig setLanguage(String language) {
        this.language = language;
        return this;
    }

    public MainConfig setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public MainConfig setNetwork(String address, Integer port, boolean encryption, long packet_size_limit) {
        this.network.set("address", address, "Bind address for the socket");
        this.network.set("port", port, "Port for the socket");
        this.network.set("encryption", encryption, "Encryption for the socket traffic");
        this.network.set("packet_size_limit", packet_size_limit, "Traffic limit in bytes");
        return this;
    }

    public MainConfig setHttpServer(String address, Integer port, String authKey) {
        this.httpServer.set("address", address, "Bind address for the http server");
        this.httpServer.set("port", port, "Port for the http server");
        this.httpServer.set("auth_key", authKey, "Authorization key for incoming HTTP requests");
        return this;
    }

    public MainConfig setMysqlSettings(String address, Integer port, String database, String user, String password) {
        this.mysqlSettings.set("address", address, "MySQL server address");
        this.mysqlSettings.set("port", port, "MySQL server port");
        this.mysqlSettings.set("database", database, "Used MySQL database");
        this.mysqlSettings.set("user", user, "Used MySQL username");
        this.mysqlSettings.set("password", password, "Used MySQL password");
        return this;
    }

    public InetSocketAddress getNetworkAddress() {
        return InetSocketAddress.createUnresolved(this.network.get("address").toString(), Integer.parseInt(this.network.get("port").toString()));
    }

    public boolean isNetworkEncryptionEnabled() {
        return this.network.get("encryption").toString().equals("true");
    }

    public long getNetworkPacketSizeLimit() {
        return Long.parseLong(this.network.get("packet_size_limit").toString());
    }

    public InetSocketAddress getHttpServerAddress() {
        return InetSocketAddress.createUnresolved(this.httpServer.get("address").toString(), Integer.parseInt(this.httpServer.get("port").toString()));
    }

    public String getHttpServerAuthKey() {
        return this.httpServer.get("auth_key").toString();
    }

    public InetSocketAddress getMysqlServerAddress() {
        return InetSocketAddress.createUnresolved(this.mysqlSettings.get("address").toString(), Integer.parseInt(this.mysqlSettings.get("port").toString()));
    }

    public String getMysqlDatabase() {
        return this.mysqlSettings.get("database").toString();
    }

    public String getMysqlUser() {
        return this.mysqlSettings.get("user").toString();
    }

    public String getMysqlPassword() {
        return this.mysqlSettings.get("password").toString();
    }

    public MySqlSettings getMysqlSettings() {
        InetSocketAddress address = getMysqlServerAddress();
        return new MySqlSettings(address.getHostString(), address.getPort(), getMysqlDatabase(), getMysqlUser(), getMysqlPassword());
    }
}