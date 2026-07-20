package de.pocketcloud.cloud.http.auth;

import de.pocketcloud.cloud.http.handler.AuthenticationFailedHandler;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;

public final class DefaultAuthFailedHandler implements AuthenticationFailedHandler {

    @Override
    public void handle(HttpRequest req, HttpResponse res) {
        res.unauthorized("Unauthorized");
    }
}