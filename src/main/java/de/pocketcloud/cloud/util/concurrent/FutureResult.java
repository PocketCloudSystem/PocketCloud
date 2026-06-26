package de.pocketcloud.cloud.util.concurrent;

public record FutureResult<T>(boolean success, T value, String error) {}