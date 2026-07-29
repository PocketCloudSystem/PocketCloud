package de.pocketcloud.cloud.http.util;

import java.util.regex.Pattern;

public record RouteDefinition(String method, String path, Pattern pattern, RouteHandlerMethod handler, int version) {

    public static String toRegex(String path) {
        return "^" + path.replaceAll("\\{([^/]+)}", "(?<$1>[^/]+)") + "$";
    }
}