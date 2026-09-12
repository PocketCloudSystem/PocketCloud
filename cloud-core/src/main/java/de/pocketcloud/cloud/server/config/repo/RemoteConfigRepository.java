package de.pocketcloud.cloud.server.config.repo;

import de.pocketcloud.cloud.console.log.CloudLogger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteConfigRepository {

    private static final String BASE_URL = "https://raw.githubusercontent.com/PocketCloudSystem/server-configs/refs/heads/main/";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final Map<String, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private RemoteConfigRepository() {}

    public record RemoteConfig(String rawContent, String version) {}

    private record CacheEntry(RemoteConfig config, Instant fetchedAt) {}

    public static RemoteConfig get(String softwareName, String fileName) {
        String key = softwareName + "/" + fileName;
        CacheEntry cached = CACHE.get(key);
        if (cached != null && Duration.between(cached.fetchedAt(), Instant.now()).compareTo(CACHE_TTL) < 0) {
            return cached.config();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + key))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                CloudLogger.get().warn("Failed to fetch remote config {}: HTTP {}", key, response.statusCode());
                return fallback(key, cached, softwareName, fileName);
            }

            String raw = response.body();
            RemoteConfig config = new RemoteConfig(raw, sha256(raw));
            CACHE.put(key, new CacheEntry(config, Instant.now()));
            return config;
        } catch (Exception e) {
            CloudLogger.get().warn("Failed to fetch remote config {}: {}", key, e.getMessage());
            return fallback(key, cached, softwareName, fileName);
        }
    }

    private static RemoteConfig fallback(String key, CacheEntry cached, String softwareName, String fileName) {
        if (cached != null) return cached.config();

        String bundled = loadBundled(softwareName, fileName);
        if (bundled != null) {
            CloudLogger.get().warn("Using bundled offline config for {} (repo unreachable)", key);
            return new RemoteConfig(bundled, "bundled:" + sha256(bundled));
        }

        return null;
    }

    private static String loadBundled(String softwareName, String fileName) {
        String resourcePath = "/" + softwareName + "/" + fileName;
        try (InputStream in = RemoteConfigRepository.class.getResourceAsStream(resourcePath)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}