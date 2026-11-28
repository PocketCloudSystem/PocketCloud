package de.pocketcloud.cloud.http.route.v1.schedule;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.cloud.http.annotation.*;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.scheduler.CommandScheduler;

/** Scheduled console commands (like the {@code schedule} command); replicated in a cluster. */
@ApiVersion(1)
public final class ScheduleRoutes {

    @GetRoute("/schedules")
    public void list(HttpRequest request, HttpResponse response) {
        JsonArray array = new JsonArray();
        CommandScheduler.all().forEach(schedule -> array.add(toJson(schedule)));
        response.json(array);
    }

    @PostRoute("/schedules")
    public void add(HttpRequest request, HttpResponse response, @RequestBody JsonObject body) {
        String cron = requireString(body, "cron"), command = requireString(body, "command");
        try {
            JsonObject json = toJson(CommandScheduler.add(cron, command));
            json.addProperty("message", "Scheduled the command.");
            response.json(json);
        } catch (IllegalArgumentException e) {
            throw new HttpException(400, "Invalid schedule: " + e.getMessage());
        }
    }

    @DeleteRoute("/schedules/{id}")
    public void remove(HttpRequest request, HttpResponse response, @PathVariable("id") String id) {
        if (!CommandScheduler.remove(id)) throw new HttpException(404, "There is no scheduled command with the id " + id + ".");
        JsonObject json = new JsonObject();
        json.addProperty("message", "Removed the scheduled command.");
        response.json(json);
    }

    private static JsonObject toJson(CommandScheduler.Schedule schedule) {
        JsonObject json = new JsonObject();
        json.addProperty("id", schedule.id());
        json.addProperty("cron", schedule.cron());
        json.addProperty("command", schedule.command());
        return json;
    }

    private static String requireString(JsonObject body, String key) {
        if (body == null || !body.has(key) || !body.get(key).isJsonPrimitive() || body.get(key).getAsString().isBlank())
            throw new HttpException(400, "Missing field: " + key);
        return body.get(key).getAsString();
    }
}
