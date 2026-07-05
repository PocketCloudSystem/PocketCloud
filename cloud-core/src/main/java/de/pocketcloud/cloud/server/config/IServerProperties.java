package de.pocketcloud.cloud.server.config;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.software.ServerSoftware;

import java.util.Map;

public interface IServerProperties {

    boolean modify(String filePath, Map<String, Object> updatedContent);

    boolean renew(String filePath);

    boolean needsRenewal(String filePath);

    Map<String, Object> replacePlaceholders(CloudServer server);

    Map<String, Object> getDefaultContent();

    String getFileName();

    ServerSoftware getServerSoftware();
}