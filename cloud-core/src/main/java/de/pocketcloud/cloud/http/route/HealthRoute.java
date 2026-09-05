package de.pocketcloud.cloud.http.route;

import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.auth.NonRequiredAuthentication;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;

public final class HealthRoute {

    @GetRoute(value = "/health", authentication = NonRequiredAuthentication.class)
    public void health(HttpRequest request, HttpResponse response) {
        response.status(200).json(obj -> obj.addProperty("status", "ok"));
    }
}