package de.pocketcloud.cloud.http.route.v1.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.http.util.HttpUtils;
import de.pocketcloud.cloud.server.CloudServer;

import java.util.Collection;

@ApiVersion(1)
public final class ServerRoutes {

    @GetRoute("/servers")
    public void list(HttpRequest request, HttpResponse response) {
        JsonObject result = new JsonObject();
        for (ICloudServer server : PocketCloud.instance().servers().getAll()) {
            result.add(server.name(), serverSummary(server));
        }

        response.json(result);
    }

    /**
     * {@link de.pocketcloud.api.search.ServerSearchQuery}
     */
    @QueryRoute("/servers")
    public void query(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        try {
            ServerSearchQuery query = ServerSearchQuery.read(HttpUtils.toMap(body));
            JsonObject json = new JsonObject();
            for (ICloudServer server : PocketCloud.instance().servers().query(query)) {
                json.add(server.name(), serverSummary(server));
            }

            response.json(json);
        } catch (ClassCastException _) {
            response.badRequest("Please use the correct data types for your values.");
        }
    }

    @GetRoute("/servers/{name}")
    public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        response.json(serverInfo(requireServer(name)));
    }

    @GetRoute("/servers/{name}/logs")
    public void logs(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        CloudServer server = requireServer(name);

        var logLines = server.retrieveLogs();
        if (logLines == null) throw new HttpException(500, "Failed to retrieve server logs.");

        response.text(String.join("\n", logLines));
    }

    @PostRoute("/servers/{name}/save")
    public void save(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        CloudServer server = requireServer(name);
        PocketCloud.instance().servers().save(server);
        response.json(message("Attempted to save the server."));
    }

    @PostRoute("/servers/{name}/dispatch")
    public void dispatch(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        CloudServer server = requireServer(name);
        String command = requireString(body, "command");

        server.dispatch(command);
        response.json(message("Attempted to dispatch the command on the server."));
    }

    @PostRoute("/servers/start")
    public void start(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String templateName = requireString(body, "template");
        int count = requireInt(body, "count");

        ITemplate template = resolveTemplate(templateName);
        if (template == null) throw new HttpException(400, "Template does not exist.");
        if (count < 1) throw new HttpException(400, "The requested amount cannot be less than 1.");

        if (!PocketCloud.instance().servers().checkCapacity(template))
            throw new HttpException(409, "The maximum amount of servers for this template has already been reached.");

        PocketCloud.instance().servers().start(template, count)
                .thenSuccess(started -> {
                    JsonObject json = message("Attempted to start " + count + " server(s).");
                    JsonArray array = new JsonArray();
                    started.forEach(array::add);
                    json.add("started_servers", array);
                    response.json(json);
                })
                .failure(e -> response.internalServerError("Failed to start servers"));
    }

    @PostRoute("/servers/stopAll")
    public void stopAll(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        boolean force = optionalBoolean(body, "force", false);

        PocketCloud.instance().servers().stopAll(force)
                .thenSuccess(servers -> response.json(toServerArray(servers).getAsJsonObject()))
                .failure(e -> response.internalServerError("Failed to stop servers"));
    }

    @PostRoute("/servers/{name}/stop")
    public void stop(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        requireServer(name);
        boolean force = optionalBoolean(body, "force", false);

        PocketCloud.instance().servers().stop(name, force)
                .thenSuccess(servers -> response.json(toServerArray(servers).getAsJsonObject()))
                .failure(e -> response.internalServerError("Failed to stop server"));
    }

    private JsonObject serverSummary(ICloudServer server) {
        JsonObject entry = new JsonObject();
        entry.addProperty("name", server.name());
        entry.addProperty("uuid", server.uuid().toString());
        entry.addProperty("status", server.status().name());
        entry.addProperty("playerCount", server.playerCount());
        entry.addProperty("maxPlayers", server.data().maxPlayers());
        return entry;
    }

    private JsonArray toServerArray(Collection<ICloudServer> servers) {
        JsonArray array = new JsonArray();
        for (ICloudServer server : servers) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", server.name());
            entry.addProperty("uuid", server.uuid().toString());
            array.add(entry);
        }
        return array;
    }

    private CloudServer requireServer(String name) {
        if (name == null || name.isBlank()) throw new HttpException(400, "Please specify a server name or uuid.");
        return PocketCloud.instance().servers().get(name)
                .map(s -> (CloudServer) s)
                .orElseThrow(() -> new HttpException(404, "Server not found."));
    }

    private ITemplate resolveTemplate(String name) {
        if (name == null) return null;
        return PocketCloud.instance().templates().get(name)
                .orElseThrow(() -> new HttpException(400, "Template does not exist."));
    }

    private IServerGroup resolveGroup(String name) {
        if (name == null) return null;
        return PocketCloud.instance().serverGroups().get(name)
                .orElseThrow(() -> new HttpException(400, "ServerGroup does not exist."));
    }

    private String requireString(JsonObject body, String key) {
        if (!body.has(key) || !body.get(key).isJsonPrimitive()) throw new HttpException(400, "Missing field: " + key);
        return body.get(key).getAsString();
    }

    private int requireInt(JsonObject body, String key) {
        if (!body.has(key) || !body.get(key).isJsonPrimitive()) throw new HttpException(400, "Missing field: " + key);
        return body.get(key).getAsInt();
    }

    private boolean optionalBoolean(JsonObject body, String key, boolean defaultValue) {
        if (body == null || !body.has(key)) return defaultValue;
        return body.get(key).getAsBoolean();
    }

    private JsonObject message(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        return json;
    }

    private JsonObject serverInfo(ICloudServer server) {
        JsonObject json = new JsonObject();
        HttpUtils.appendWritable(server, json);
        json.addProperty("playerCount", server.playerCount());
        json.add("players", HttpUtils.toElement(server.players().stream().map(ICloudPlayer::name).toList()));
        return json;
    }
}