<?php

namespace pocketcloud\cloud\util;

use Closure;

final class FormatUtils {

    public static function bytes(int $bytes): string {
        if ($bytes < 1024) return $bytes . " B";

        $units = ["B", "KB", "MB", "GB", "TB", "PB", "EB"];
        $exp = (int) floor(log($bytes, 1024));
        $value = $bytes / (1024 ** $exp);
        $value = round($value, 2);

        return $value . " " . $units[$exp];
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
        } elseif ($s >= 1) {
            return round($s, $precision) . "s";
        } elseif ($s >= 0.001) {
            return round($s * 1000, $precision) . "ms";
        } elseif ($s >= 0.000001) {
            return round($s * 1_000_000, $precision) . "µs";
        } else {
            return round($s * 1_000_000_000, $precision) . "ns";
        }
    }

    public static function milliseconds(float $ms, int $precision = 3): string {
        if ($ms >= 1000) {
            return round($ms / 1000, $precision) . "s";
        } elseif ($ms >= 1) {
            return round($ms, $precision) . "ms";
        } elseif ($ms >= 0.001) {
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
     * @return string
     */
    public static function implodeWithKeys(array $array, string $separator = ", ", string $keyValueSeparator = ": ", ?Closure $keyProcessHandler = null, ?Closure $valueProcessHandler = null): string {
        $parts = [];

        foreach ($array as $key => $value) {
            $actualKey = $key;
            if ($keyProcessHandler !== null) $key = $keyProcessHandler($key);
            if ($valueProcessHandler !== null) $value = $valueProcessHandler($actualKey, $value);
            $parts[] = $key . $keyValueSeparator . $value;
        }

        return implode($separator, $parts);
    }
}