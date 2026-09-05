package de.pocketcloud.cloud.http.route.v1.plugin;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.annotation.ApiVersion;
import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.annotation.PostRoute;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.plugin.CloudPlugin;

@ApiVersion(1)
public final class PluginRoutes {

    @GetRoute("/plugins")
    public void list(HttpRequest request, HttpResponse response) {
        boolean enabledOnly = "true".equalsIgnoreCase(request.queryParam("enabled", "false"));
        
        JsonObject json = new JsonObject();
        for (var plugin : (enabledOnly ? PocketCloud.instance().plugins().getEnabledPlugins() : PocketCloud.instance().plugins().getPlugins().values())) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", plugin.getDescription().name());
            entry.addProperty("version", plugin.getDescription().version());
            JsonArray authors = new JsonArray();
            for (String author : plugin.getDescription().authors()) authors.add(author);
            entry.add("authors", authors);
            json.add(plugin.getDescription().name(), entry);
        }

        response.json(json);
    }

    @GetRoute("/plugins/{name}")
    public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        var plugin = requirePlugin(name);

        JsonObject json = new JsonObject();
        json.addProperty("name", plugin.getDescription().name());
        json.addProperty("status", plugin.isEnabled() ? "enabled" : "disabled");
        json.addProperty("version", plugin.getDescription().version());
        json.addProperty("main", plugin.getDescription().main());
        json.addProperty("dataFolder", plugin.getDataFolder().toAbsolutePath().toString());
        response.json(json);
    }

    @PostRoute("/plugins/{name}/enable")
    public void enable(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        var plugin = requirePlugin(name);
        if (plugin.isEnabled()) {
            response.json(message("Plugin is already enabled."));
            return;
        }

        PocketCloud.instance().plugins().enable(plugin);
        response.json(message("Plugin has been enabled."));
    }

    @PostRoute("/plugins/{name}/disable")
    public void disable(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        var plugin = requirePlugin(name);
        if (plugin.isDisabled()) {
            response.json(message("Plugin is already disabled."));
            return;
        }

        PocketCloud.instance().plugins().disable(plugin);
        response.json(message("Plugin has been disabled."));
    }

    @PostRoute("/plugins/enableAll")
    public void enableAll(HttpRequest request, HttpResponse response) {
        PocketCloud.instance().plugins().enableAll();
        response.json(message("All plugins have been enabled."));
    }

    @PostRoute("/plugins/disableAll")
    public void disableAll(HttpRequest request, HttpResponse response) {
        PocketCloud.instance().plugins().disableAll();
        response.json(message("All plugins have been disabled."));
    }

    private CloudPlugin requirePlugin(String name) {
        if (name == null || name.isBlank()) throw new HttpException(400, "Please specify a plugin name.");
        return PocketCloud.instance().plugins().get(name)
                .orElseThrow(() -> new HttpException(404, "Plugin not found."));
    }

    private JsonObject message(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        return json;
    }
}