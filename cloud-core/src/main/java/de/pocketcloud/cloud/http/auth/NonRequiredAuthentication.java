package de.pocketcloud.cloud.http.auth;

import de.pocketcloud.cloud.http.io.HttpRequest;

public final class NonRequiredAuthentication implements IAuthentication {

    @Override
    public boolean authenticated(HttpRequest request) {
        return true;
    }
}