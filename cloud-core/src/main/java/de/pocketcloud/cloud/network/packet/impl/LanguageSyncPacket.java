package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.language.Language;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.common.util.FileUtils;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

@NoArgsConstructor
@Getter
public final class LanguageSyncPacket extends CloudPacket implements ClientboundPacket {

    private String language;
    private Map<String, String> messages;

    public LanguageSyncPacket(String language, Map<String, String> messages) {
        this.language = language != null ? language : "";
        this.messages = messages != null ? messages : Map.of();
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        try {
            byte[] compressed = gzipCompress(FileUtils.GSON.toJson(messages).getBytes(StandardCharsets.UTF_8));
            String encoded = Base64.getEncoder().encodeToString(compressed);
            packetData.writeAll(language, encoded);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode LanguageSyncPacket", e);
        }
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    private byte[] gzipCompress(byte[] data) throws Exception {
        var baos = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
        }
        return baos.toByteArray();
    }

    public static LanguageSyncPacket create(String language, Map<String, String> messages) {
        return new LanguageSyncPacket(language, messages);
    }

    public static LanguageSyncPacket fromLanguage(Language language) {
        Language lang = language != null ? language : Language.current();
        return new LanguageSyncPacket(lang.name(), lang.messages());
    }

    public static LanguageSyncPacket fromLanguage() {
        return fromLanguage(null);
    }
}
