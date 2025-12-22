<?php

namespace pocketcloud\cloud\server\binary;

use Exception;
use PharData;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\net\NetUtils;
use ZipArchive;
use const pocketcloud\BINARIES_PATH;

final class BinaryDownloader {

    public static function downloadBinary(string $downloadUrl, string $templateType): bool {
        if (file_exists(BINARIES_PATH . "$templateType/")) return true;
        CloudLogger::get()->info("Downloading §b$templateType binaries §8(§b{}§8)§r...", $downloadUrl);

        return ExceptionHandler::tryCatch(function (string $downloadUrl, string $templateType): bool {
            if (NetUtils::download($downloadUrl, BINARIES_PATH . ($fileName = basename($downloadUrl)))) {
                if (file_exists(BINARIES_PATH . $fileName)) {
                    $extractionPath = BINARIES_PATH . "$templateType/";
                    $extension = pathinfo(BINARIES_PATH . $fileName, PATHINFO_EXTENSION);
                    switch (strtolower($extension)) {
                        case "zip": {
                            $zip = new ZipArchive();
                            if ($zip->open($downloadUrl, ZipArchive::CREATE)) {
                                $zip->extractTo($extractionPath);
                                $zip->close();
                            }
                            break;
                        }
                        default: {
                            $phar = new PharData(BINARIES_PATH . $fileName);
                            $phar->extractTo($extractionPath, null, true);
                        }
                    }

                    unlink(BINARIES_PATH . $fileName);
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