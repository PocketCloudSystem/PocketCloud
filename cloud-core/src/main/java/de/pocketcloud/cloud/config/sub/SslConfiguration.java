package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.File;

@Getter
@Setter
@Accessors(fluent = true)
public final class SslConfiguration extends OkaeriConfig implements ICloudConfig {

    @Comment({"Whether HTTPS/TLS should be enabled for the HTTP server"})
    private boolean enabled = false;

    @Comment({"If true, a self-signed certificate is generated automatically at startup.", "Useful for local development. Ignores certPath/privateKeyPath when true."})
    private boolean selfSigned = false;

    @CustomKey("cert-path")
    @Comment({"Path to the fullchain/certificate PEM file"})
    private String certPath = "ssl/fullchain.pem";

    @CustomKey("private-key-path")
    @Comment({"Path to the private key PEM file"})
    private String privateKeyPath = "ssl/privkey.pem";

    @CustomKey("key-password")
    @Comment({"Password for the private key, leave empty if not password protected"})
    private String keyPassword = "";

    @CustomKey("self-signed-hostname")
    @Comment({"Hostname (CN) used when generating a self-signed certificate"})
    private String selfSignedHostname = "localhost";

    @Override
    public void validate() {
        if (!enabled || selfSigned) return;

        File cert = new File(certPath);
        File key = new File(privateKeyPath);

        if (!cert.exists() || !key.exists()) {
            PocketCloud.instance().appendStartNotification(
                    "SSL is enabled but certificate or private key file was not found. Disabling SSL...",
                    CloudLogLevel.WARN
            );

            enabled = false;
        }
    }

    public File certFile() {
        return new File(certPath);
    }

    public File privateKeyFile() {
        return new File(privateKeyPath);
    }

    public boolean hasKeyPassword() {
        return keyPassword != null && !keyPassword.isBlank();
    }
}