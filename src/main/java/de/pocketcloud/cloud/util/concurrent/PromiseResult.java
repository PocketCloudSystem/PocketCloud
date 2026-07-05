package de.pocketcloud.cloud.util.concurrent;

public record PromiseResult<T>(boolean success, T value, Throwable error) {}