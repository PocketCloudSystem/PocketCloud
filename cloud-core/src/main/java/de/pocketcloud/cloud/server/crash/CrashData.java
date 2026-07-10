package de.pocketcloud.cloud.server.crash;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record CrashData(boolean crashed,
                        @Nullable String errorType,
                        @Nullable String message,
                        @Nullable String file,
                        @Nullable Integer line,
                        @Nullable List<String> trace,
                        @Nullable String plugin
) {}