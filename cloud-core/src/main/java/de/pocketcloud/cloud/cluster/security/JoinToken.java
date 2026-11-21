package de.pocketcloud.cloud.cluster.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pocketcloud.common.util.FileUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Everything a new node needs to join: its name, the cluster CA certificate, its own certificate
 * and key, and the addresses of existing nodes. Encoded as base64url JSON with a {@code pcjoin1.} prefix.
 * <p>
 * The token contains a private key. Treat it like a password and use it only once.
 */
public record JoinToken(String nodeName, String caCertificatePem, String nodeCertificatePem, String nodeKeyPem, List<String> seeds) {

    private static final String PREFIX = "pcjoin1.";

    public String encode() {
        JsonObject json = new JsonObject();
        json.addProperty("name", nodeName);
        json.addProperty("ca", caCertificatePem);
        json.addProperty("cert", nodeCertificatePem);
        json.addProperty("key", nodeKeyPem);
        json.add("seeds", FileUtils.GSON.toJsonTree(seeds));
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static JoinToken decode(String token) {
        String trimmed = token.trim();
        if (!trimmed.startsWith(PREFIX)) throw new IllegalArgumentException("Not a PocketCloud join token");
        String json = new String(Base64.getUrlDecoder().decode(trimmed.substring(PREFIX.length())), StandardCharsets.UTF_8);
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
        List<String> seeds = new ArrayList<>();
        obj.getAsJsonArray("seeds").forEach(e -> seeds.add(e.getAsString()));
        return new JoinToken(obj.get("name").getAsString(), obj.get("ca").getAsString(), obj.get("cert").getAsString(), obj.get("key").getAsString(), seeds);
    }
}
