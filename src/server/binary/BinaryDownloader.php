<?php

namespace pocketcloud\cloud\server\binary;

use Exception;
use PharData;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\net\NetUtils;
use RuntimeException;
use ZipArchive;
use const pocketcloud\BINARIES_PATH;

final class BinaryDownloader {

    public static function downloadBinary(string $downloadUrl, string $templateType): ?bool {
        if (file_exists(BINARIES_PATH . "$templateType/") && array_diff(scandir(BINARIES_PATH . "$templateType/"), [".", ".."]) > 0) return null;
        @mkdir(BINARIES_PATH . "$templateType/");
        CloudLogger::get()->info("Downloading §b$templateType binaries §8(§b{}§8)§r...", $downloadUrl);

        return ExceptionHandler::tryCatch(function (string $downloadUrl, string $templateType): bool {
            if (NetUtils::download($downloadUrl, BINARIES_PATH . ($fileName = basename($downloadUrl)))) {
                if (file_exists(BINARIES_PATH . $fileName)) {
                    $knownFileNames = [$fileName];
                    $extractionPath = BINARIES_PATH . "$templateType/";
                    $extension = pathinfo(BINARIES_PATH . $fileName, PATHINFO_EXTENSION);
                    switch (strtolower($extension)) {
                        case "zip": {
                            CloudLogger::get()->info("Using 'ZipArchive' to extract...");
                            sleep(1);
                            $zip = new ZipArchive();
                            if ($zip->open($downloadUrl, ZipArchive::CREATE)) {
                                $zip->extractTo($extractionPath);
                                $zip->close();
                            }
                            break;
                        }
                        case "gz": {
                            if (str_ends_with($fileName, ".tar.gz")) {
                                CloudLogger::get()->info("Using 'shell' to extract...");
                                sleep(1);
                                $cmd = sprintf(
                                    "tar -xzf %s -C %s",
                                    escapeshellarg(BINARIES_PATH . $fileName),
                                    escapeshellarg(BINARIES_PATH . $templateType)
                                );

                                exec($cmd, $output, $code);

                                if ($code !== 0) throw new RuntimeException("tar extraction failed");
                                break;
                            }
                        }
                        default: {
                            CloudLogger::get()->info("Using 'PharData' to extract...");
                            sleep(1);
                            $phar = new PharData(BINARIES_PATH . $fileName);
                            $phar->extractTo($extractionPath, null, true);
                        }
                    }

                    foreach ($knownFileNames as $fileName) unlink(BINARIES_PATH . $fileName);

                    if (!file_exists($extractionPath . "bin/")) {
                        throw new Exception("Failed to extract $templateType binaries");
                    }

                    return true;
                }
            }

            throw new Exception("Failed to download $templateType binaries");
        }, "§cFailed to download §b$templateType binaries §8(§b" . $downloadUrl . "§8)", function (): void {
            PocketCloud::getInstance()->shutdown();
        }, $downloadUrl, $templateType) ?? false;
    }
}