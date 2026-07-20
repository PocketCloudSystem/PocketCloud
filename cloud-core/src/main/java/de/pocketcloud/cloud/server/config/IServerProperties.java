package de.pocketcloud.cloud.server.config;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.server.CloudServer;

import java.util.Map;

public interface IServerProperties {

    boolean modify(String filePath, Map<String, Object> updatedContent);

    boolean renew(String filePath);

    boolean needsRenewal(String filePath);

    Map<String, Object> replacePlaceholders(CloudServer server);

    Map<String, Object> getDefaultContent();

    String getFileName();

    IServerSoftware getServerSoftware();
}