<?php

namespace pocketcloud\cloud\util;

use FilesystemIterator;
use InvalidArgumentException;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use RecursiveDirectoryIterator;
use RecursiveIteratorIterator;
use RuntimeException;
use const pocketcloud\CLOUD_PATH;

final class FileUtils {

    public static function copyFile(string $src, string $dst): bool {
        return ExceptionHandler::tryCatch(
            function (string $src, string $dst): bool {
                if (!@file_exists($src)) return false;
                return copy($src, $dst);
            },
            "Failed to copy " . $src . " to " . $dst,
            null,
            $src, $dst
        );
    }

    public static function createDir(string $path): bool {
        return ExceptionHandler::tryCatch(
            function (string $path): bool {
                if (@file_exists($path)) return true;
                return mkdir($path, 0777, true);
            },
            "Failed to create directory: " . $path,
            null,
            $path
        ) ?? false;
    }

    public static function filePutContents(string $filePath, string $content): int|false {
        return ExceptionHandler::tryCatch(
            function (string $filePath, string $content): int|false {
                $previousPath = dirname($filePath);
                $return = self::createDir($previousPath);
                if (!$return) return false;
                return file_put_contents($filePath, $content);
            },
            "Failed to write in file: " . $filePath,
            null,
            $filePath, $content
        ) ?? false;
    }

    public static function fileGetContents(string $filePath, string $default = ""): ?string {
        return ExceptionHandler::tryCatch(
            function (string $filePath, mixed $default): string {
                if (!@file_exists($filePath)) return $default;
                return file_get_contents($filePath);
            },
            "Failed to read file: " . $filePath,
            null,
            $filePath, $default
        );
    }

    public static function copyDirectory(string $src, string $dst): bool {
        return ExceptionHandler::tryCatch(
            function (string $src, string $dst): bool {
                if (!is_dir($src)) throw new InvalidArgumentException("Source directory does not exist: $src");

                if (!is_dir($dst) && !mkdir($dst, 0777, true)) throw new RuntimeException("Cannot create destination directory: $dst");
                $iterator = new RecursiveIteratorIterator(
                    new RecursiveDirectoryIterator($src, FilesystemIterator::SKIP_DOTS),
                    RecursiveIteratorIterator::SELF_FIRST
                );

                foreach ($iterator as $item) {
                    $relativePath = substr($item->getPathname(), strlen($src));
                    $dstPath = $dst . $relativePath;

                    if ($item->isDir()) {
                        if (!is_dir($dstPath)) mkdir($dstPath, 0755, true);
                    } else {
                        copy($item->getPathname(), $dstPath);
                    }
                }

                return true;
            },
            null,
            null,
            $src, $dst
        );
    }

    public static function rename(string $src, string $dst): bool {
        return ExceptionHandler::tryCatch(
            function (string $src, string $dst): bool {
                return rename($src, $dst);
            },
            "Failed to rename: " . $src . " to " . $dst,
            null,
            $src, $dst
        );
    }

    public static function unlinkFile(string $filePath): bool {
        return ExceptionHandler::tryCatch(
            fn() => unlink($filePath),
            "Failed to unlink file: " . $filePath
        ) ?? false;
    }

    public static function removeDirectory(string $directoryPath): bool {
        return ExceptionHandler::tryCatch(
            function (string $directoryPath): bool {
                if (@is_dir($directoryPath)) {
                    $iterator = new RecursiveIteratorIterator(
                        new RecursiveDirectoryIterator($directoryPath, FilesystemIterator::SKIP_DOTS),
                        RecursiveIteratorIterator::CHILD_FIRST
                    );

                    foreach ($iterator as $file) {
                        if ($file->isDir()) {
                            rmdir($file->getRealPath());
                        } else {
                            unlink($file->getPathname());
                        }
                    }

                    return rmdir($directoryPath);
                }
                return false;
            },
            "Failed to remove directory: " . $directoryPath,
            null,
            $directoryPath
        ) ?? false;
    }

    public static function encodeJson(array $jsonArray, int $flags = 0, int $depth = 512): ?string {
        return ExceptionHandler::tryCatch(
            function (array $jsonArray, int $flags, int $depth): ?string {
                $encode = json_encode($jsonArray, JSON_THROW_ON_ERROR | $flags, $depth);
                return is_string($encode) ? $encode : null;
            },
            "Failed to encode json string",
            null,
            $jsonArray, $flags, $depth
        );
    }

    public static function encodeJsonFile(string $filePath, array $jsonArray, int $flags = 0, int $depth = 512): ?bool {
        return ExceptionHandler::tryCatch(
            function (string $filePath, array $jsonArray, int $flags, int $depth): ?string {
                $encode = json_encode($jsonArray, JSON_THROW_ON_ERROR | $flags, $depth);
                if (is_string($encode)) return is_int(file_put_contents($filePath, $encode));
                return is_string($encode) ? $encode : null;
            },
            "Failed to encode & place json string into a file",
            null,
            $filePath, $jsonArray, $flags, $depth
        );
    }

    public static function decodeJson(string $jsonString, int $depth = 512, int $flags = 0): ?array {
        return ExceptionHandler::tryCatch(
            function (string $jsonString, int $depth, int $flags): ?array {
                $decode = json_decode($jsonString, true, $depth, JSON_THROW_ON_ERROR | $flags);
                return is_array($decode) ? $decode : null;
            },
            "Failed to decode json string: " . $jsonString,
            null,
            $jsonString, $depth, $flags
        );
    }

    public static function decodeJsonFile(string $filePath, int $depth = 512, int $flags = 0): ?array {
        return ExceptionHandler::tryCatch(
            function (string $filePath, int $depth, int $flags): ?array {
                $decode = json_decode(file_get_contents($filePath), true, $depth, JSON_THROW_ON_ERROR | $flags);
                return is_array($decode) ? $decode : null;
            },
            "Failed to decode json file: " . $filePath,
            null,
            $filePath, $depth, $flags
        );
    }

    public static function emitYaml(mixed $yamlData, int $encoding = YAML_ANY_ENCODING, int $linebreak = YAML_ANY_BREAK): string {
        return ExceptionHandler::tryCatch(
            function (mixed $yamlData, int $encoding, int $linebreak): ?string {
                $emitted = yaml_emit($yamlData, $encoding, $linebreak);
                return is_string($emitted) ? $emitted : null;
            },
            "Failed to emit yaml data",
            null,
            $yamlData, $encoding, $linebreak
        );
    }

    public static function emitYamlFile(string $filePath, mixed $yamlData, int $encoding = YAML_ANY_ENCODING, int $linebreak = YAML_ANY_BREAK): bool {
        return ExceptionHandler::tryCatch(
            function (string $filePath, mixed $yamlData, int $encoding, int $linebreak): bool {
                return yaml_emit_file($filePath, $yamlData, $encoding, $linebreak);
            },
            "Failed to emit & place yaml string into file",
            null,
            $filePath, $yamlData, $encoding, $linebreak
        );
    }

    public static function parseYaml(string $yamlString): ?array {
        return ExceptionHandler::tryCatch(
            function (string $yamlString): ?array {
                $parsed = yaml_parse($yamlString);
                return is_array($parsed) ? $parsed : null;
            },
            "Failed to parse yaml string: " . $yamlString,
            null,
            $yamlString
        );
    }

    public static function parseYamlFile(string $filePath): ?array {
        return ExceptionHandler::tryCatch(
            function (string $filePath): ?array {
                $parsed = yaml_parse_file($filePath);
                return is_array($parsed) ? $parsed : null;
            },
            "Failed to parse yaml file: " . $filePath,
            null,
            $filePath
        );
    }

    public static function cleanPath(string $path, bool $removePath = false): string {
        if ($removePath) return ($explode = explode(DIRECTORY_SEPARATOR, str_replace(["\\", "//", DIRECTORY_SEPARATOR], DIRECTORY_SEPARATOR, $path)))[count($explode) - 1];
        $result = str_replace([".php", "phar://"], ["", ""], $path);
        $cleanPath = rtrim(str_replace("phar://", "", CLOUD_PATH), DIRECTORY_SEPARATOR);
        if (str_starts_with($result, $cleanPath)) $result = ltrim(str_replace($cleanPath, "pcsrc", $result), DIRECTORY_SEPARATOR);
        return $result;
    }
}