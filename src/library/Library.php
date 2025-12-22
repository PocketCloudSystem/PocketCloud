<?php

namespace pocketcloud\cloud\library;

use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\net\NetUtils;
use pocketcloud\cloud\util\Utils;
use ZipArchive;
use const pocketcloud\LIBRARIES_PATH;

final readonly class Library {

    private string $libPath;

    public function __construct(
        private string $name,
        private string $downloadUrl,
        private string $namespacePrefix,
        private string $namespaceFolder,
        private bool $bridgeOnly
    ) {
        $this->libPath = LIBRARIES_PATH . $this->name . DIRECTORY_SEPARATOR;
    }

    public function download(): bool {
        if ($this->check()) return false;
        return ExceptionHandler::tryCatch(
            function (string $name, string $downloadUrl, string $libPath): bool {
                CloudLogger::get()->debug("Downloading source for library: {}" . $name . "...");
                NetUtils::download($downloadUrl, $archivePath = LIBRARIES_PATH . uniqid());
                $archive = new ZipArchive();
                if ($archive->open($archivePath)) {
                    $archive->extractTo(LIBRARIES_PATH);
                    $mainDir = rtrim($archive->getNameIndex(0), "/");
                    $archive->close();

                    if (!file_exists($libPath)) mkdir($libPath);
                    FileUtils::rename(LIBRARIES_PATH . $mainDir . "/", $libPath);
                }

                unlink($archivePath);
                return true;
            },
            "Failed to download library: " . $this->name,
            null,
            $this->name, $this->downloadUrl, $this->libPath
        ) ?? false;
    }

    public function load(): bool {
        if (!$this->check()) return false;
        if ($this->bridgeOnly) return false;
        CloudLogger::get()->debug("Loading library: {} into class loader...", $this->name);
        PocketCloud::getInstance()->getClassLoader()->addPrefix($this->namespacePrefix, LIBRARIES_PATH . $this->name . DIRECTORY_SEPARATOR . $this->namespaceFolder);
        return true;
    }

    public function check(): bool {
        return file_exists($this->libPath) &&
            is_dir($this->libPath) &&
            count(scandir($this->libPath)) > 0 &&
            @file_exists($this->libPath . $this->namespaceFolder);
    }

    public function getLibPath(): string {
        return $this->libPath;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getDownloadUrl(): string {
        return $this->downloadUrl;
    }

    public function getNamespacePrefix(): string {
        return $this->namespacePrefix;
    }

    public function getNamespaceFolder(): string {
        return $this->namespaceFolder;
    }

    public function isBridgeOnly(): bool {
        return $this->bridgeOnly;
    }

    public function write(): array {
        return [
            "name" => $this->name,
            "downloadUrl" => $this->downloadUrl,
            "namespacePrefix" => $this->namespacePrefix,
            "namespaceFolder" => $this->namespaceFolder,
            "bridgeOnly" => $this->bridgeOnly
        ];
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, "name", "downloadUrl", "namespacePrefix", "namespaceFolder", "bridgeOnly")) return null;
        return new self($data["name"], $data["downloadUrl"], $data["namespacePrefix"], $data["namespaceFolder"], $data["bridgeOnly"]);
    }
}