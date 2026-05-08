package de.pocketcloud.cloud.util;

public final class FormatUtils {

    public static String interpolate(String message, Object[] params) {
        for (Object param : params) message = message.replaceFirst("\\{}", String.valueOf(param));
        return message;
    }
}