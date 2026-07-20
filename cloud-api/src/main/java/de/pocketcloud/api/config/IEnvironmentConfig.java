package de.pocketcloud.api.config;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.UUID;

public interface IEnvironmentConfig {

    SocketAddress cloudNetworkAddress();

    SocketAddress localNetworkAddress();

    String networkAuthKey();

    boolean networkEncryption();

    int networkPacketSizeLimit();

    String localServerName();

    UUID localServerUuid();

    String localTemplateName();

    int localServerTimeout();

    Path cloudPath();

    String language();
}