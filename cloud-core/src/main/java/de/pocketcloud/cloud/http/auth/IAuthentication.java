package de.pocketcloud.cloud.http.auth;

import de.pocketcloud.cloud.http.io.HttpRequest;

public interface IAuthentication {

    boolean authenticated(HttpRequest request);
}