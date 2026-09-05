package de.pocketcloud.cloud.http.route.v1.player;

import com.google.gson.JsonObject;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.search.PlayerSearchQuery;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.http.util.HttpUtils;
import de.pocketcloud.shared.network.packet.type.TextType;

@ApiVersion(1)
public final class PlayerRoutes {

    @GetRoute("/players")
    public void list(HttpRequest request, HttpResponse response) {
        JsonObject result = new JsonObject();
        for (ICloudPlayer player : PocketCloud.instance().players().getAll()) {
            result.add(player.name(), playerSummary(player));
        }

        response.json(result);
    }

    /**
     * {@link de.pocketcloud.api.search.PlayerSearchQuery}
     */
    @QueryRoute("/players")
    public void query(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        try {
            PlayerSearchQuery query = PlayerSearchQuery.read(HttpUtils.toMap(body));
            JsonObject json = new JsonObject();
            for (ICloudPlayer player : PocketCloud.instance().players().query(query)) {
                json.add(player.name(), playerSummary(player));
            }

            response.json(json);
        } catch (ClassCastException _) {
            response.badRequest("Please use the correct data types for your values.");
        }
    }

    @GetRoute("/players/{name}")
    public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        ICloudPlayer player = requirePlayer(name);
        response.json(playerInfo(player));
    }

    @PostRoute("/players/{name}/kick")
    public void kick(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        ICloudPlayer player = requirePlayer(name);
        String reason = optionalString(body, "reason", "");
        String disconnectScreenMessage = optionalString(body, "disconnectScreenMessage", "");

        player.kick(reason, disconnectScreenMessage);
        response.json(message("Kicked the player."));
    }

    @PostRoute("/players/{name}/text")
    public void text(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        ICloudPlayer player = requirePlayer(name);
        String typeName = requireString(body, "type");
        String messageTitle = optionalString(body, "title", "");
        String messageBody = optionalString(body, "body", !messageTitle.isEmpty() ? messageTitle : "");
        int fadeIn = optionalInt(body, "fadeIn", 1);
        int stay = optionalInt(body, "stay", 0);
        int fadeOut = optionalInt(body, "fadeOut", 1);

        TextType type;
        try {
            type = TextType.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new HttpException(400, "Invalid type name: " + typeName);
        }

        switch (type) {
            case MESSAGE -> player.sendMessage(messageBody);
            case POPUP -> player.sendPopup(messageTitle, messageBody);
            case TIP -> player.sendTip(messageBody);
            case TITLE -> player.sendTitle(messageTitle, messageBody);
            case ACTION_BAR -> player.sendActionbarMessage(messageBody, fadeIn, stay, fadeOut);
            case TOAST -> player.sendToast(messageTitle, messageBody);
        }

        response.json(message("Attempted to text the player."));
    }

    @PostRoute("/players/{name}/transfer")
    public void transfer(HttpRequest request, HttpResponse response, @PathVariable("name") String name, @RequestBody JsonObject body) {
        ICloudPlayer player = requirePlayer(name);
        String serverName = requireString(body, "server");

        ICloudServer server = PocketCloud.instance().servers().get(serverName)
                .orElseThrow(() -> new HttpException(400, "Server not found."));

        player.transfer(server);
        response.json(message("Attempted to transfer the player."));
    }

    private ICloudPlayer requirePlayer(String name) {
        if (name == null || name.isBlank()) throw new HttpException(400, "Please specify a player name.");
        return PocketCloud.instance().players().get(name)
                .orElseThrow(() -> new HttpException(404, "Player not found."));
    }

    private String requireString(JsonObject body, String key) {
        if (!body.has(key) || !body.get(key).isJsonPrimitive()) throw new HttpException(400, "Missing field: " + key);
        return body.get(key).getAsString();
    }

    private String optionalString(JsonObject body, String key, String defaultValue) {
        if (body == null || !body.has(key)) return defaultValue;
        return body.get(key).getAsString();
    }

    private Integer optionalInt(JsonObject body, String key, Integer defaultValue) {
        if (body == null || !body.has(key)) return defaultValue;
        return body.get(key).getAsInt();
    }

    private JsonObject message(String message) {
        JsonObject json = new JsonObject();
        json.addProperty("message", message);
        return json;
    }

    private JsonObject playerSummary(ICloudPlayer player) {
        JsonObject json = new JsonObject();
        json.addProperty("name", player.name());
        json.addProperty("xboxUserId", player.xboxUserId());
        json.addProperty("server", player.currentServerName());
        json.addProperty("proxy", player.currentProxyName());
        return json;
    }
    
    private JsonObject playerInfo(ICloudPlayer player) {
        JsonObject json = new JsonObject();
        json.addProperty("name", player.name());
        json.addProperty("uniqueId", player.uniqueId().toString());
        json.addProperty("xboxUserId", player.xboxUserId());
        json.addProperty("protocolVersion", player.protocolVersion());
        json.addProperty("gameVersion", player.gameVersion());
        json.addProperty("server", player.currentServerName());
        json.addProperty("proxy", player.currentProxyName());
        return json;
    }
}