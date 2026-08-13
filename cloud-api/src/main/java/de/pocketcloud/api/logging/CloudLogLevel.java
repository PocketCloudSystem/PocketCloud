package de.pocketcloud.api.logging;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public enum CloudLogLevel {

    INFO("§bINFO"),
    WARN("§cWARN"),
    ERROR("§4ERROR"),
    SUCCESS("§aSUCCESS"),
    DEBUG("§6DEBUG");

    public static final CloudLogLevel LONGEST = SUCCESS;

    private final String prefix;

    CloudLogLevel(String prefix) {
        this.prefix = prefix;
    }

    public static String padPrefixToLength(CloudLogLevel level) {
        return " ".repeat(Math.max(0, (LONGEST.prefix.length() - level.prefix.length()))) + level.prefix;
    }
}