<?php

namespace pocketcloud\cloud\util;

use const pocketcloud\CLOUD_PATH;

final class PathUtils {

    public static function normalize(string $path): string {
        return rtrim(str_replace(["\\", "//"], "/", $path), "/");
    }

    public static function join(string ...$paths): string {
        $cleanedPaths = [];
        foreach ($paths as $path) {
            if (trim($path) == "") continue;
            if (empty($clean)) {
                $cleanedPaths[] = self::normalize($path);
                continue;
            }

            $cleanedPaths[] = trim(self::normalize($path), "/");
        }

        return implode("/", $cleanedPaths);
    }

    public static function clean(string $path, bool $removePath = false): string {
        $path = self::normalize($path);
        if ($removePath) return ($parts = explode("/", $path))[count($parts) - 1];
        $result = str_replace([".php", "phar://"], "", $path);
        $cleanRootPath = rtrim(str_replace("phar://", "", CLOUD_PATH), "/");
        if (str_starts_with($result, $cleanRootPath)) $result = ltrim(str_replace($cleanRootPath, "pcsrc", $result), "/");
        return $result;
    }
}