package de.pocketcloud.cloud.util;

public final class TimeUtils {

    public static long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    public static double currentTime() {
        return currentTimeMillis() / 1000.0;
    }

    public static long currentSeconds() {
        return currentTimeMillis() / 1000L;
    }
}