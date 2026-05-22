package de.pocketcloud.cloud.event;

import java.util.function.Consumer;

public record RegisteredHandler(EventPriority priority, Consumer<Event> handler) {}