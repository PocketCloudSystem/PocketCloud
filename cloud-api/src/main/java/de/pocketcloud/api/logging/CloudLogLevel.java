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

    private final String prefix;

    CloudLogLevel(String prefix) {
        this.prefix = prefix;
    }
}