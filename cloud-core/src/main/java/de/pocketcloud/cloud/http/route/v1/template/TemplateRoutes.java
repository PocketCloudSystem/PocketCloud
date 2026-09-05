package de.pocketcloud.cloud.http.route.v1.template;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.TemplateSearchQuery;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.builder.TemplateBuilder;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.http.util.HttpUtils;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.common.util.FileUtils;

import java.util.Map;

@ApiVersion(1)
public final class TemplateRoutes {

    @GetRoute("/templates")
    public void list(HttpRequest request, HttpResponse response) {
        JsonObject json = new JsonObject();
        for (ITemplate template : PocketCloud.instance().templates().getAll()) {
            json.add(template.name(), templateSummary(template));
        }

        response.json(json);
    }

    /**
     * {@link de.pocketcloud.api.search.TemplateSearchQuery}
     */
    @QueryRoute("/templates")
    public void query(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        try {
            TemplateSearchQuery query = TemplateSearchQuery.read(HttpUtils.toMap(body));
            JsonObject json = new JsonObject();
            for (ITemplate template : PocketCloud.instance().templates().query(query)) {
                json.add(template.name(), templateSummary(template));
            }

            response.json(json);
        } catch (ClassCastException _) {
            response.badRequest("Please use the correct data types for your values.");
        }
    }

    @GetRoute("/templates/{name}")
    public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        response.json(templateInfo(requireTemplate(name)));
    }

    @PostRoute("/templates")
    public void create(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String name = requireString(body, "name");
        if (PocketCloud.instance().templates().check(name)) throw new HttpException(409, "Template already exists.");

        Template template = Template.read(FileUtils.GSON.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType()));
        PocketCloud.instance().templates().create(TemplateBuilder.of(template));
        response.json(message("Created the template."));
    }

    @PatchRoute("/templates/{name}")
    public void edit(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        try {
            ITemplate template = requireTemplate(name);
            TemplateEditData editData = TemplateEditData.read(FileUtils.GSON.fromJson(body, new TypeToken<Map<String, Object>>() {}.getType()));
            PocketCloud.instance().templates().edit(template, editData);
            response.json(message("Edited the template."));
        } catch (ClassCastException _) {
            response.badRequest("Please use the correct data types for your values.");
        }
    }

    @DeleteRoute("/templates/{name}")
    public void remove(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        ITemplate template = requireTemplate(name);
        PocketCloud.instance().templates().remove(template);
        response.json(message("Removed the template."));
    }

    private ITemplate requireTemplate(String name) {
        if (name == null || name.isBlank()) throw new HttpException(400, "Please specify a template name.");
        return PocketCloud.instance().templates().get(name).orElseThrow(() -> new HttpException(404, "Template not found."));
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

    private JsonObject templateInfo(ITemplate template) {
        JsonObject json = new JsonObject();
        HttpUtils.appendWritable(template, json);
        json.addProperty("playerCount", template.playerCount());
        json.add("players", HttpUtils.toElement(template.players().stream().map(ICloudPlayer::name).toList()));
        json.add("parentGroups", HttpUtils.toElement(template.parentGroups().stream().map(IServerGroup::name).toList()));
        return json;
    }

    private JsonObject templateSummary(ITemplate template) {
        JsonObject json = new JsonObject();
        json.addProperty("name", template.name());
        json.addProperty("playerCount", template.playerCount());
        json.addProperty("maxPlayers", template.settings().maxPlayerCount());
        json.addProperty("serverCount", template.serverCount());
        json.addProperty("maxServers", template.settings().maxServerCount());
        json.addProperty("lobby", template.settings().lobby());
        json.addProperty("maintenance", template.settings().maintenance());
        json.addProperty("software", template.serverSoftware().name());
        return json;
    }
}