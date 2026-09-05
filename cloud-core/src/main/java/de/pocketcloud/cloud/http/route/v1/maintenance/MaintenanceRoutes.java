package de.pocketcloud.cloud.http.route.v1.maintenance;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.common.cache.LocalCache;

import java.util.Map;

@ApiVersion(1)
public final class MaintenanceRoutes {

    @GetRoute("/maintenance")
    public void list(HttpRequest request, HttpResponse response) {
        JsonArray json = new JsonArray();
        for (String player : LocalCache.get(WhitelistCache.class).getAll().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList()) json.add(player);
        response.json(json);
    }

    @PostRoute("/maintenance")
    public void add(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String player = requireString(body, "player");
        CloudProvider.current().addToWhitelist(player);
        response.json(message("Player has been added to the whitelist."));
    }

    @DeleteRoute("/maintenance")
    public void remove(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String player = requireString(body, "player");
        CloudProvider.current().removeFromWhitelist(player);
        response.json(message("Player has been removed from the whitelist."));
    }

    @GetRoute("/maintenance/{name}")
    public void check(HttpRequest request, HttpResponse response, @PathVariable("name") String player) {
        response.json(obj -> obj.addProperty("whitelisted", LocalCache.get(WhitelistCache.class).get(player).orElse(false)));
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