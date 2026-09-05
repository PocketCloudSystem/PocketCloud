package de.pocketcloud.cloud.http.route.v1.notification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.common.cache.LocalCache;

import java.util.Map;

@ApiVersion(1)
public final class NotificationRoutes {

    @GetRoute("/notifications")
    public void list(HttpRequest request, HttpResponse response) {
        JsonArray json = new JsonArray();
        for (String player : LocalCache.get(NotificationListCache.class).getAll().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList()) json.add(player);
        response.json(json);
    }

    @PostRoute("/notifications")
    public void enable(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String player = requireString(body, "player");
        CloudProvider.current().enablePlayerNotifications(player);
        response.json(message("Player's notifications have been enabled."));
    }

    @DeleteRoute("/notifications")
    public void disable(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String player = requireString(body, "player");
        CloudProvider.current().disablePlayerNotifications(player);
        response.json(message("Player's notifications have been disabled."));
    }

    @GetRoute("/notifications/{name}")
    public void check(HttpRequest request, HttpResponse response, @PathVariable("name") String player) {
        response.json(obj -> obj.addProperty("enabled", LocalCache.get(NotificationListCache.class).get(player).orElse(false)));
    }

    private String requireString(JsonObject body, String key) {
        if (!body.has(key) || !body.get(key).isJsonPrimitive()) throw new HttpException(400, "Missing field: " + key);
        return body.get(key).getAsString();
    }

    private JsonObject message(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        return json;
    }
}