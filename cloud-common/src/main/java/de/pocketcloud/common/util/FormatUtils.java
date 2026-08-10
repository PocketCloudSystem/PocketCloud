package de.pocketcloud.common.util;

import java.util.regex.Matcher;

public final class FormatUtils {

    public static String uptime(double seconds) {
        int days = 0;
        int hours = 0;
        int minutes = 0;

        while (seconds >= 86400) {
            days++;
            seconds -= 86400;
        }

        while (seconds >= 3600) {
            hours++;
            seconds -= 3600;
        }

        while (seconds >= 60) {
            minutes++;
            seconds -= 60;
        }

        return (days > 0 ? days + "d, " : "") +
                (hours > 0 ? hours + "h, " : "") +
                (minutes > 0 ? minutes + "m, " : "") +
                (seconds > 0 ? (int) Math.floor(seconds) + "s" : "");
    }

    public static String tps(Double tps) {
        return tps(tps, true, true);
    }

    public static String tps(Double tps, boolean coloured) {
        return tps(tps, true, coloured);
    }

    public static String tps(Double tps, boolean suffix, boolean coloured) {
        if (tps == null || tps < 0) return (coloured ? "§c" : "") + "???";
        tps = round(tps, 2);

        String actualSuffix = suffix ? " ticks/s" : "";

        if (coloured) {
            if (tps >= 17) {
                return "§a" + tps + actualSuffix;
            } else if (tps >= 12) {
                return "§6" + tps + actualSuffix;
            }

            return "§c" + tps + actualSuffix;
        } else return tps + actualSuffix;
    }

    public static String bytes(Long bytes) {
        return bytes(bytes, null, false, true);
    }

    public static String bytes(Long bytes, boolean coloured) {
        return bytes(bytes, null, false, coloured);
    }

    public static String bytes(Long bytes, boolean higherPctBetter, boolean coloured) {
        return bytes(bytes, null, higherPctBetter, coloured);
    }

    public static String bytes(Long bytes, Long maxBytes, boolean higherPctBetter, boolean coloured) {
        if (bytes == null || bytes < 0) return (coloured ? "§c" : "") + "???";
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB"};
        int exp = bytes > 0 ? (int) Math.floor(Math.log(bytes) / Math.log(1024)) : 0;
        double value = bytes / Math.pow(1024, exp);
        value = round(value, 2);
        String formatted = value + " " + units[exp];
        if (maxBytes == null || maxBytes <= 0) return formatted;
        double percent = ((double) bytes / maxBytes) * 100;

        String color = "";
        if (coloured) {
            if (percent < 60) {
                color = !higherPctBetter ? "§a" : "§c";
            } else if (percent < 85) {
                color = "§e";
            } else {
                color = higherPctBetter ? "§a" : "§c";
            }
        }

        String format = "%s (%s%.1f%%)";
        if (coloured) format = "§b%s §8(%s%.1f%%§8)§r";
        return String.format(format, formatted, color, percent);
    }

    public static String downloadSpeed(Double bytesPerSecond) {
        if (bytesPerSecond == null || Double.isNaN(bytesPerSecond) || Double.isInfinite(bytesPerSecond) || bytesPerSecond <= 0) return "N/A";
        if (bytesPerSecond >= Math.pow(1024, 3)) return String.format("%.1f GB/s", bytesPerSecond / Math.pow(1024, 3));
        if (bytesPerSecond >= Math.pow(1024, 2)) return String.format("%.1f MB/s", bytesPerSecond / Math.pow(1024, 2));
        if (bytesPerSecond >= 1024) return String.format("%.1f KB/s", bytesPerSecond / 1024);
        return String.format("%.0f B/s", bytesPerSecond);
    }

    public static String usagePercentage(Double percentage) {
        return usagePercentage(percentage, false, 3, true);
    }

    public static String usagePercentage(Double percentage, boolean coloured) {
        return usagePercentage(percentage, false, 3, coloured);
    }

    public static String usagePercentage(Double percentage, boolean higherBetter, boolean coloured) {
        return usagePercentage(percentage, higherBetter, 3, coloured);
    }

    public static String usagePercentage(Double percentage, boolean higherBetter, int precision, boolean coloured) {
        if (percentage == null || percentage < 0) return (coloured ? "§c" : "") + "???";
        String formatted = round(percentage, precision) + "%";
        String color = "";
        if (coloured) {
            if (percentage < 60) {
                color = higherBetter ? "§c" : "§a";
            } else if (percentage < 85) {
                color = "§e";
            } else {
                color = higherBetter ? "§a" : "§c";
            }
        }

        return color + formatted;
    }

    public static String interpolate(String subject, Object... args) {
        for (Object arg : args) subject = subject.replaceFirst("\\{}", Matcher.quoteReplacement(String.valueOf(arg)));
        return subject;
    }

    public static String seconds(double s) {
        return seconds(s, 3);
    }

    public static String seconds(double s, int precision) {
        if (s >= 60) {
            return round(s / 60, precision) + "min";
        } else if (s >= 1) {
            return round(s, precision) + "s";
        } else if (s >= 0.001) {
            return round(s * 1000, precision) + "ms";
        } else if (s >= 0.000001) {
            return round(s * 1_000_000, precision) + "µs";
        } else {
            return round(s * 1_000_000_000, precision) + "ns";
        }
    }

    public static String milliseconds(double ms) {
        return milliseconds(ms, 3);
    }

    public static String milliseconds(double ms, int precision) {
        if (ms >= 1000) {
            return round(ms / 1000, precision) + "s";
        } else if (ms >= 1) {
            return round(ms, precision) + "ms";
        } else if (ms >= 0.001) {
            return round(ms * 1000, precision) + "µs";
        } else {
            return round(ms * 1_000_000, precision) + "ns";
        }
    }

    private static double round(double value, int precision) {
        double scale = Math.pow(10, precision);
        return Math.round(value * scale) / scale;
    }
}