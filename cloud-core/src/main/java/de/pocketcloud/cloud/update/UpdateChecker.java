package de.pocketcloud.cloud.update;

import de.pocketcloud.common.concurrent.Promise;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {

    private static final String API_URL = "https://api.github.com/repos/PocketCloudSystem/PocketCloud/releases/tags/latest-core";
    private static final Pattern VERSION_PATTERN = Pattern.compile("\\(([\\d.]+)\\)");

    public record UpdateResult(boolean updateAvailable, String currentVersion, String latestVersion, Instant updateReleasedAt) {}

    public static Promise<UpdateResult> check(String currentVersion) {
        Promise<UpdateResult> promise = new Promise<>();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        promise.reject(new IOException("GitHub API returned status " + response.statusCode()));
                        return;
                    }

                    JsonNode json = new ObjectMapper().readTree(response.body());
                    String name = json.get("name").asString();
                    Matcher matcher = VERSION_PATTERN.matcher(name);
                    if (!matcher.find()) {
                        promise.reject(new IOException("Unable to extract version from string: " + name));
                        return;
                    }

                    Instant updatedAt = Instant.parse(json.get("updated_at").asString());
                    String latestVersion = matcher.group(1);
                    boolean isNewer = compareVersions(latestVersion, currentVersion) > 0;
                    promise.resolve(new UpdateResult(isNewer, currentVersion, latestVersion, updatedAt));
                })
                .exceptionally(ex -> {
                    promise.reject(ex);
                    return null;
                });

        return promise;
    }

    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }
}