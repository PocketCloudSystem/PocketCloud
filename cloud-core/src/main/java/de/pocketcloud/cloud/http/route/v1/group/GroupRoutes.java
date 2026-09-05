package de.pocketcloud.cloud.http.route.v1.group;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.ServerGroupSearchQuery;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.builder.ServerGroupBuilder;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.http.util.HttpUtils;
import de.pocketcloud.cloud.template.group.ServerGroup;

@ApiVersion(1)
public final class GroupRoutes {

    @GetRoute("/groups")
    public void list(HttpRequest request, HttpResponse response) {
        JsonObject json = new JsonObject();
        for (IServerGroup group : PocketCloud.instance().serverGroups().getAll()) {
            json.add(group.name(), groupSummary(group));
        }

        response.json(json);
    }

    /**
     * {@link de.pocketcloud.api.search.ServerGroupSearchQuery}
     */
    @QueryRoute("/groups")
    public void query(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        try {
            ServerGroupSearchQuery query = ServerGroupSearchQuery.read(HttpUtils.toMap(body));
            JsonObject json = new JsonObject();
            for (IServerGroup group : PocketCloud.instance().serverGroups().query(query)) {
                json.add(group.name(), groupSummary(group));
            }

            response.json(json);
        } catch (ClassCastException _) {
            response.badRequest("Please use the correct data types for your values.");
        }
    }

    @GetRoute("/groups/{name}")
    public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        response.json(groupInfo(requireGroup(name)));
    }

    @PostRoute("/groups")
    public void create(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String name = requireString(body, "name");
        if (PocketCloud.instance().serverGroups().check(name)) {
            throw new HttpException(409, "Group already exists.");
        }

        IServerGroup group = ServerGroup.read(HttpUtils.toMap(body));
        PocketCloud.instance().serverGroups().create(ServerGroupBuilder.of(group));
        response.json(message("Created the group."));
    }

    @DeleteRoute("/groups/{name}")
    public void remove(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        IServerGroup group = requireGroup(name);
        PocketCloud.instance().serverGroups().remove(group);
        response.json(message("Removed the group."));
    }

    @PostRoute("/groups/{name}/templates")
    public void addTemplates(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        IServerGroup group = requireGroup(name);
        for (var element : requireArray(body, "templates")) {
            if (!element.isJsonPrimitive()) continue;
            ITemplate template = PocketCloud.instance().templates().get(element.getAsString()).orElse(null);
            if (template == null) continue;
            PocketCloud.instance().serverGroups().addTemplate(group, template);
        }

        response.json(message("Added the templates to the group."));
    }

    @DeleteRoute("/groups/{name}/templates")
    public void removeTemplates(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        IServerGroup group = requireGroup(name);
        for (var element : requireArray(body, "templates")) {
            if (!element.isJsonPrimitive()) continue;
            ITemplate template = PocketCloud.instance().templates().get(element.getAsString()).orElse(null);
            if (template == null) continue;
            PocketCloud.instance().serverGroups().removeTemplate(group, template);
        }

        response.json(message("Removed the templates from the group."));
    }

    private IServerGroup requireGroup(String name) {
        if (name == null || name.isBlank()) throw new HttpException(400, "Please specify a group name.");
        return PocketCloud.instance().serverGroups().get(name)
                .orElseThrow(() -> new HttpException(404, "Group not found."));
    }

    private String requireString(JsonObject body, String key) {
        if (!body.has(key) || !body.get(key).isJsonPrimitive()) throw new HttpException(400, "Missing field: " + key);
        return body.get(key).getAsString();
    }

    private JsonArray requireArray(JsonObject body, String key) {
        if (!body.has(key) || !body.get(key).isJsonArray()) throw new HttpException(400, "Missing field: " + key);
        return body.getAsJsonArray(key);
    }

    private JsonObject message(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        return json;
    }

    private JsonObject groupSummary(IServerGroup group) {
        JsonObject json = new JsonObject();
        json.addProperty("name", group.name());
        json.addProperty("playerCount", group.playerCount());
        return json;
    }

    private JsonObject groupInfo(IServerGroup group) {
        JsonObject json = new JsonObject();
        HttpUtils.appendWritable(group, json);
        json.add("players", HttpUtils.toElement(group.players()));
        json.addProperty("player_count", group.playerCount());
        return json;
    }
}