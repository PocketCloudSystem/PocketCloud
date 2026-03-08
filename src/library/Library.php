<?php

namespace pocketcloud\cloud\library;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\update\UpdateChecker;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\net\NetUtils;
use pocketcloud\cloud\util\PathUtils;
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
        $this->libPath = PathUtils::join(LIBRARIES_PATH, $this->name) . "/";
    }

    /**
     * When downloaded a library, the cloud expects the zip Archive to have the following structure:
     * -> library.zip -> Library-main (or any name) -> the actual library contents (src, readme.md, ...)
     * @param bool $needsUpdate
     * @return bool
     * @internal
     */
    public function download(bool $needsUpdate = false): bool {
        if ($needsUpdate && !MainConfig::getInstance()->canUpdate(UpdateChecker::TYPE_LIBRARIES)) {
            Server::getInstance()->addStartNotification("Library §b{} §rrequires an §cUPDATE§r, but inside your §bconfig.yml§r, you have §cdisabled §8'§eexecuteUpdates§8'§r.", CloudLogLevel::WARN(), $this->name);
            Server::getInstance()->addStartNotification("Please §are-enable §rit or update the library manually.", CloudLogLevel::WARN());
            return false;
        }

        if (@is_dir($this->libPath)) FileUtils::removeDirectory($this->libPath);
        return ExceptionHandler::require(
            function (string $name, string $downloadUrl, string $libPath): bool {
                CloudLogger::get()->info("Downloading source for library: {}...", $name);
                $size = NetUtils::download($downloadUrl, $archivePath = PathUtils::join(LIBRARIES_PATH, uniqid()));
                $archive = new ZipArchive();
                if ($archive->open($archivePath)) {
                    $mainPath = rtrim($archive->getNameIndex(0), "/");
                    $archive->extractTo(LIBRARIES_PATH);
                    $archive->close();

                    if (!is_dir($libPath)) mkdir($libPath);
                    FileUtils::rename(PathUtils::join(LIBRARIES_PATH, $mainPath) . "/", $libPath);
                    file_put_contents(PathUtils::join($libPath, ".size"), $size);
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
        Server::getInstance()->getClassLoader()->addPrefix($this->namespacePrefix, LIBRARIES_PATH . $this->name . "/" . $this->namespaceFolder);
        return true;
    }

    public function check(): bool {
        return file_exists($this->libPath) &&
            is_dir($this->libPath) &&
            count(scandir($this->libPath)) > 0 &&
            @is_dir(PathUtils::join($this->libPath, $this->namespaceFolder)) &&
            @file_exists(PathUtils::join($this->libPath, ".size"));
    }

    public function needsAnUpdate(): bool {
        if (!@file_exists(PathUtils::join($this->libPath, ".size"))) return true;
        $lastDownloadSize = intval(file_get_contents(PathUtils::join($this->libPath, ".size")));
        $newestDownloadSize = NetUtils::fileSize($this->downloadUrl);
        return $newestDownloadSize !== $lastDownloadSize;
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