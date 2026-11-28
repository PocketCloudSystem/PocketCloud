package de.pocketcloud.cloud.http.route.v1.backup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.cloud.backup.BackupService;
import de.pocketcloud.cloud.http.annotation.ApiVersion;
import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.annotation.PostRoute;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/** The backups of the node that answers (like the {@code backup} command). The zips themselves are not served: they contain secrets. */
@ApiVersion(1)
public final class BackupRoutes {

    @GetRoute("/backups")
    public void list(HttpRequest request, HttpResponse response) {
        JsonArray array = new JsonArray();
        try {
            for (Path zip : BackupService.list()) {
                JsonObject json = new JsonObject();
                json.addProperty("name", zip.getFileName().toString());
                json.addProperty("time", zip.toFile().lastModified());
                json.addProperty("size", Files.size(zip));
                array.add(json);
            }
        } catch (IOException e) {
            throw new HttpException(500, "Could not list the backups: " + e.getMessage());
        }
        response.json(array);
    }

    @PostRoute("/backups")
    public void create(HttpRequest request, HttpResponse response) {
        Thread.ofVirtual().start(() -> {
            try {
                Path zip = BackupService.create("backup");
                JsonObject json = new JsonObject();
                json.addProperty("message", "Created the backup " + zip.getFileName() + ".");
                json.addProperty("name", zip.getFileName().toString());
                response.json(json);
            } catch (Exception e) {
                response.internalServerError("Could not create the backup: " + e.getMessage());
            }
        });
    }

    @PostRoute("/backups/{name}/restore")
    public void restore(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        Thread.ofVirtual().start(() -> {
            try {
                String before = BackupService.restore(name);
                JsonObject json = new JsonObject();
                json.addProperty("message", "Restored " + name + ". The previous state was saved as " + before + ". Restart this node to load the restored configs.");
                json.addProperty("before", before);
                response.json(json);
            } catch (IllegalArgumentException | NoSuchFileException e) {
                response.badRequest("No such backup: " + name);
            } catch (Exception e) {
                response.internalServerError("Could not restore " + name + ": " + e.getMessage());
            }
        });
    }
}
