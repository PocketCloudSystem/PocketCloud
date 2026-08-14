package de.pocketcloud.api.event;

import java.util.function.Consumer;

public record RegisteredHandler(Class<?> eventClass, EventPriority priority, Consumer<Event> handler) {}