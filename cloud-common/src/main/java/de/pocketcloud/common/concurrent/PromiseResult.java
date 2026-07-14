package de.pocketcloud.common.concurrent;

public record PromiseResult<T>(boolean success, T value, Throwable error) {}