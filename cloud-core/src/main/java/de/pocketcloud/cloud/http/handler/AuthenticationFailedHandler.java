package de.pocketcloud.cloud.http.handler;

import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;

@FunctionalInterface
public interface AuthenticationFailedHandler {

    void handle(HttpRequest req, HttpResponse res);
}