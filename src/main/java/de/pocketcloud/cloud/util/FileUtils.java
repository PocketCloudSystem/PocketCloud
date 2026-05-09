package de.pocketcloud.cloud.util;

import java.nio.file.Path;

public final class FileUtils {

    public static String extensionOf(Path path) {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf(".");
        String extension = null;
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) extension = fileName.substring(dotIndex + 1);
        return extension;
    }
}