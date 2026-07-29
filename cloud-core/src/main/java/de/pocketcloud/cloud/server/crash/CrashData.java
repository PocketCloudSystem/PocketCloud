package de.pocketcloud.cloud.server.crash;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.util.TimeUtils;
import org.jetbrains.annotations.Nullable;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record CrashData(boolean crashed,
                        String serverName,
                        UUID serverUuid,
                        @Nullable String errorType,
                        @Nullable String message,
                        @Nullable String file,
                        @Nullable Integer line,
                        @Nullable List<String> trace,
                        @Nullable String plugin
) implements Writable<Map<String, Object>> {

    public void printStackTrace() {
        if (!crashed) return;
        String message = "";
        if (errorType != null) message += "§cUnhandled §e" + errorType + "§c";
        else message += "§cUnknown error§8(§b?§8) §coccurred";

        if (this.message != null) message += ": §e" + this.message;

        if (file != null) {
            message += " in §e" + file + " §cat line §e" + line;
        }

        CloudLogger.get().error("§8[§cERROR§8/§e{}§r§8] §r§c{}", serverName, message);

        if (trace != null) {
            for (String line : trace) {
                CloudLogger.get().error("§c" + line);
            }
        }
    }

    public void writeFile() {
        Path path = PocketCloudPaths.storage().crashes().server().with(serverName + "_" + serverUuid.toString() + "_" + TimeUtils.currentSeconds() + ".txt").asPath();
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write("Error: " + Objects.requireNonNullElse(errorType, "Unknown error type") + "\n");
            fileWriter.write("Message: " + Objects.requireNonNullElse(message, "Unknown error message") + "\n");
            fileWriter.write("File: " + Objects.requireNonNullElse(message, "Unknown file") + "\n");
            fileWriter.write("Line: " + Objects.requireNonNullElse(message, "Unknown line") + "\n");
            fileWriter.write("Trace:");
            if (trace != null) {
                for (String line : trace) {
                    fileWriter.write("\n" + line);
                }
            }

            fileWriter.flush();
        } catch (IOException e) {
            CloudLogger.get().exception("Unable to write crash file for §b{}§r.", e, serverName);
        }
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static CrashData read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, CrashData.class);
    }

    public static CrashData noCrash(ICloudServer server) {
        return new CrashData(false, server.name(), server.uuid(), null, null, null, null, null, null);
    }

    public static CrashData noInfo(ICloudServer server, boolean crashed) {
        return new CrashData(crashed, server.name(), server.uuid(), null, null, null, null, null, null);
    }
}