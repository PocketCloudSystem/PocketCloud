package de.pocketcloud.cloud.server.util;

import de.pocketcloud.api.template.TemplateType;

public record ServerPortRange(TemplateType type, int start, int end, boolean random) {

    public boolean inRange(int port) {
        return port >= start && port <= end;
    }
}