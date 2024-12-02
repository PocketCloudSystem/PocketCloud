<?php

namespace pocketcloud\cloud\util;

use pocketcloud\cloud\exception\ExceptionHandler;

final class FileUtils {

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
            params: $filePath
        );
    }

    public static function copyDirectory(string $sourceLocation, string $destinationLocation): bool {
        return ExceptionHandler::tryCatch(
            function (string $srcLocation, string $dstLocation): bool {
                $srcLocation = rtrim($srcLocation, "/");
                $dstLocation = rtrim($dstLocation, "/");
                if (@is_dir($srcLocation)) {
                    $dir = opendir($srcLocation);
                    @mkdir($dstLocation);
                    while ($file = readdir($dir)) {
                        if (($file != ".") && ($file != "..")) {
                            if (is_dir($srcLocation . "/" . $file))  {
                                self::copyDirectory($srcLocation . "/" . $file, $dstLocation . "/" . $file);
                            } else {
                                copy($srcLocation . "/" . $file, $dstLocation . "/" . $file);
                            }
                        }
                    }
                    closedir($dir);
                }
                return false;
            },
            null,
            null,
            $sourceLocation, $destinationLocation
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