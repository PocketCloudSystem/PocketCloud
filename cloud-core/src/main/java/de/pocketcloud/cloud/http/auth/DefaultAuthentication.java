package de.pocketcloud.cloud.http.auth;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.common.util.StringUtils;

public final class DefaultAuthentication implements IAuthentication {

    @Override
    public boolean authenticated(HttpRequest request) {
        return request.bearerToken()
                .map(token -> StringUtils.secureEquals(token, PocketCloud.instance().httpServer().authToken()))
                .orElse(false);
    }
}