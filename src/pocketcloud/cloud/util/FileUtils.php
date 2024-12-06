<?php

namespace pocketcloud\cloud\util;

use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\terminal\log\CloudLogger;
use Throwable;

final class FileUtils {

    public static function copyFile(string $src, string $dst): bool {
        return ExceptionHandler::tryCatch(
            function (string $src, string $dst): bool {
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
                if (is_dir($path)) return true;
                $previousPath = substr($path, 0, strrpos($path, DIRECTORY_SEPARATOR, -2) + 1);
                $return = self::createDir($previousPath);
                return $return && is_writable($previousPath) && mkdir($path);
            },
            "Failed to create directory: " . $path,
            null,
            $path
        ) ?? false;
    }

    public static function filePutContents(string $filePath, string $content): int|false {
        return ExceptionHandler::tryCatch(
            function (string $filePath, string $content): int|false {
                return file_put_contents($filePath, $content);
            },
            "Failed to write in file: " . $filePath,
            null,
            $filePath, $content
        ) ?? false;
    }

    public static function fileGetContents(string $filePath): ?string {
        return ExceptionHandler::tryCatch(
            function (string $filePath): string {
                return file_get_contents($filePath);
            },
            "Failed to read file: " . $filePath,
            null,
            $filePath
        );
    }

    public static function copyDirectory(string $src, string $dst): bool {
        return ExceptionHandler::tryCatch(
            function (string $src, string $dst): bool {
                $src = rtrim($src, DIRECTORY_SEPARATOR);
                $dst = rtrim($dst, DIRECTORY_SEPARATOR);
                self::createDir($src);
                self::createDir($dst);

                foreach (array_diff(scandir($src), [".", ".."]) as $file) {
                    try {
                        if (filetype($src . DIRECTORY_SEPARATOR . $file) == "dir") {
                            self::copyDirectory($src . DIRECTORY_SEPARATOR . $file, $dst . DIRECTORY_SEPARATOR . $file);
                        } else {
                            try {
                                copy($src . DIRECTORY_SEPARATOR . $file, $dst . DIRECTORY_SEPARATOR . $file);
                            } catch (Throwable) {
                                CloudLogger::get()->debug("Can't copy file from: " . $src . DIRECTORY_SEPARATOR . $file . " to " . $dst . DIRECTORY_SEPARATOR . $file);
                            }
                        }
                    } catch (Throwable) {}
                }
                return false;
            },
            null,
            null,
            $src, $dst
        );
    }

    public static function unlinkFile(string $filePath): bool {
        return ExceptionHandler::tryCatch(
            fn() => @unlink($filePath),
            "Failed to unlink file: " . $filePath
        ) ?? false;
    }

    public static function removeDirectory(string $directoryPath): bool {
        return ExceptionHandler::tryCatch(
            function (string $directoryPath): bool {
                if (@is_dir($directoryPath)) {
                    foreach (array_diff(scandir($directoryPath), [".", ".."]) as $file) {
                        $filePath = rtrim($directoryPath, "/") . "/" . $file;
                        if (is_file($filePath)) self::unlinkFile($filePath);
                        else if (is_dir($filePath)) self::removeDirectory($filePath);
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

    public static function jsonDecode(string $jsonString, int $depth = 512): ?array {
        return ExceptionHandler::tryCatch(
            function (string $jsonString, int $depth): ?array {
                $decode = json_decode($jsonString, true, $depth);
                return !$decode ? null : $decode;
            },
            "Failed to json decode: " . $jsonString,
            null,
            $jsonString, $depth
        );
    }

    public static function jsonEncode(array $jsonArray): ?string {
        return ExceptionHandler::tryCatch(
            function (array $jsonArray): ?string {
                $encode = json_encode($jsonArray);
                return !$encode ? null : $encode;
            },
            "Failed to json encode",
            null,
            $jsonArray
        );
    }
}