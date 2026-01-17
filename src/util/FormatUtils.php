<?php

namespace pocketcloud\cloud\util;

use Closure;

final class FormatUtils {

    public static function tps(float $tps): string {
        $tps = round($tps, 2);
        if ($tps < 0) return "§c???";
        else if ($tps >= 17) return "§a" . $tps . " ticks/s";
        else if ($tps >= 12) return "§6" . $tps . " ticks/s";
        return "§c" . $tps . " ticks/s";
    }

    public static function bytes(int $bytes, ?int $maxBytes = null): string {
        if ($bytes < 0) return "§c???";

        $units = ["B", "KB", "MB", "GB", "TB", "PB", "EB"];
        $exp = $bytes > 0 ? (int) floor(log($bytes, 1024)) : 0;
        $value = $bytes / (1024 ** $exp);
        $value = round($value, 2);
        $formatted = $value . " " . $units[$exp];

        if ($maxBytes === null || $maxBytes <= 0) return $formatted;

        $percent = ($bytes / $maxBytes) * 100;
        if ($percent < 60) {
            $color = "§a";
        } else if ($percent < 85) {
            $color = "§e";
        } else {
            $color = "§c";
        }

        return sprintf("%s%s (%.1f%%)", $color, $formatted, $percent);
    }

    public static function usagePercentage(float $percentage, bool $higherBetter = false): string {
        if ($percentage < 0) return "§c???";

        $formatted = round($percentage, 3) . "%";
        if ($percentage < 60) {
            $color = ($higherBetter ? "§c" : "§a");
        } else if ($percentage < 85) {
            $color = "§e";
        } else {
            $color = ($higherBetter ? "§a" : "§c");
        }

        return sprintf("%s%s", $color, $formatted);
    }

    public static function interpolate(string $subject, array $args, string $pattern = "{}"): string {
        foreach ($args as $arg) {
            $pos = strpos($subject, $pattern);
            if ($pos === false) break;
            $subject = substr_replace($subject, (string) $arg, $pos, strlen($pattern));
        }

        return $subject;
    }

    public static function seconds(float $s, int $precision = 3): string {
        if ($s >= 60) {
            return round($s / 60, $precision) . "min";
        } else if ($s >= 1) {
            return round($s, $precision) . "s";
        } else if ($s >= 0.001) {
            return round($s * 1000, $precision) . "ms";
        } else if ($s >= 0.000001) {
            return round($s * 1_000_000, $precision) . "µs";
        } else {
            return round($s * 1_000_000_000, $precision) . "ns";
        }
    }

    public static function milliseconds(float $ms, int $precision = 3): string {
        if ($ms >= 1000) {
            return round($ms / 1000, $precision) . "s";
        } else if ($ms >= 1) {
            return round($ms, $precision) . "ms";
        } else if ($ms >= 0.001) {
            return round($ms * 1000, $precision) . "µs";
        } else {
            return round($ms * 1_000_000, $precision) . "ns";
        }
    }

    /**
     * @param array $array
     * @param string $separator
     * @param string $keyValueSeparator
     * @param Closure(mixed $key): mixed|null $keyProcessHandler
     * @param Closure(mixed $processedKey, mixed $value): mixed|null $valueProcessHandler
     * @param string ...$ignoredKeys
     * @return string
     */
    public static function implodeWithKeys(array $array, string $separator = ", ", string $keyValueSeparator = ": ", ?Closure $keyProcessHandler = null, ?Closure $valueProcessHandler = null, string ...$ignoredKeys): string {
        $parts = [];

        foreach ($array as $key => $value) {
            if (in_array($key, $ignoredKeys)) continue;
            $actualKey = $key;
            if ($keyProcessHandler !== null) $key = $keyProcessHandler($key);
            if ($valueProcessHandler !== null) $value = $valueProcessHandler($actualKey, $value);
            $parts[] = $key . $keyValueSeparator . $value;
        }

        return implode($separator, $parts);
    }
}