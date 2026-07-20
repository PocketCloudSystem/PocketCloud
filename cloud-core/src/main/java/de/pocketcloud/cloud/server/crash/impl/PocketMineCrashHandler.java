package de.pocketcloud.cloud.server.crash.impl;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.crash.CrashData;
import de.pocketcloud.cloud.server.crash.CrashHandler;
import de.pocketcloud.cloud.server.software.ServerSoftwareManager;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.util.FileUtils;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public final class PocketMineCrashHandler implements CrashHandler {

    public static final String LOG_EXTENSION = "log";

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public CrashData retrieveCrashData(CloudServer server) {
        Path path = server.path().resolve("crashdumps/");
        TemplateType type = server.template().templateType();
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path filePath : stream) {
                    if (!filePath.getFileName().toString().endsWith(LOG_EXTENSION)) continue;
                    File file = filePath.toFile();
                    if ((System.currentTimeMillis() - file.lastModified()) <= ((TemplateTypeHelper.timeout(type) + 1) * 1000L)) {
                        Map<String, Object> data = readData(filePath);
                        if (data == null || data.isEmpty()) return new CrashData(true, null, null, null, null, null, null);
                        Map<String, Object> errorData = (Map<String, Object>) data.getOrDefault("error", new HashMap<>());
                        if (errorData == null || errorData.isEmpty()) return new CrashData(true, null, null, null, null, null, null);
                        return new CrashData(
                                true,
                                errorData.getOrDefault("type", "Unknown").toString(),
                                errorData.getOrDefault("message", "Unknown message").toString(),
                                errorData.getOrDefault("file", "Unknown file").toString(),
                                Integer.parseInt(errorData.getOrDefault("line", -1).toString()),
                                (ArrayList<String>) data.getOrDefault("trace", new ArrayList<>()),
                                data.getOrDefault("plugin", "None").toString()
                        );
                    }
                }
            } catch (IOException | DataFormatException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private Map<String, Object> readData(Path file) throws IOException, DataFormatException {
        List<String> lines = Files.readAllLines(file);

        boolean start = false;
        StringBuilder data = new StringBuilder();
        byte[] finalData;

        for (String line : lines) {
            line = line.trim();

            if (start) {
                if (line.equals("===END CRASH DUMP===")) break;
                data.append(line);
            } else if (line.equals("===BEGINN CRASH DUMP===")) {
                start = true;
            }
        }

        finalData = Base64.getDecoder().decode(data.toString());
        Inflater inflater = new Inflater();
        inflater.setInput(finalData);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            output.write(buffer, 0, count);
        }

        inflater.end();

        finalData = output.toByteArray();
        return FileUtils.GSON.fromJson(new String(finalData), new TypeToken<Map<String, Object>>(){}.getType());
    }

    @Override
    public List<IServerSoftware> applicableSoftware() {
        return List.of(ServerSoftwareManager.DEFAULTS.getFirst());
    }
}