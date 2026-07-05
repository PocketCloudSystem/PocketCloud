package de.pocketcloud.cloud.http.route;

import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.annotation.PostRoute;
import de.pocketcloud.cloud.http.annotation.RequestBody;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;

public final class TestRoutes {

    @GetRoute("/test/{id}")
    public void test(HttpRequest request, HttpResponse response, @PathVariable("id") String id) {
        response.status(200).json(obj -> obj.addProperty("id", id));
    }

    @PostRoute("/test/")
    public void post(HttpRequest request, HttpResponse response, @RequestBody String body) {
        response.status(200).json(obj -> obj.addProperty("body", body));
    }
}