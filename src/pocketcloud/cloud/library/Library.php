<?php

namespace pocketcloud\cloud\library;

use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\net\NetUtils;
use ZipArchive;

final readonly class Library {

    public function __construct(
        private string $name,
        private string $downloadUrl,
        private string $fileLocation,
        private string $unzipLocation,
        private ?string $classLoadFolder,
        private ?string $classLoadPath,
        private array $excludedFiles = [],
        private string $copySource = "",
        private string $copyDestination = "",
        private ?string $deletionDir = null,
        private bool $cloudBridgeOnly = false
    ) {}

    public function download(): bool {
        if (!NetUtils::download($this->downloadUrl, $this->fileLocation)) return false;
        $archive = new ZipArchive();
        if ($archive->open($this->fileLocation)) {
            if (!file_exists($this->unzipLocation)) mkdir($this->unzipLocation);
            $archive->extractTo($this->unzipLocation);
            if ($this->copySource !== "" && $this->copyDestination !== "") {
                FileUtils::copyDirectory($this->copySource, $this->copyDestination);
                foreach ($this->excludedFiles as $excludedFile) {
                    if (file_exists($this->copyDestination . DIRECTORY_SEPARATOR . $excludedFile)) unlink($this->copyDestination . DIRECTORY_SEPARATOR . $excludedFile);
                }
            } else {
                foreach ($this->excludedFiles as $excludedFile) {
                    if (file_exists($this->unzipLocation . DIRECTORY_SEPARATOR . $excludedFile)) unlink($this->unzipLocation . DIRECTORY_SEPARATOR . $excludedFile);
                }
            }
        }

        @unlink($this->fileLocation);
        if ($this->copySource !== "") FileUtils::removeDirectory($this->deletionDir ?? $this->copySource);
        return true;
    }

    public function exists(): bool {
        if (@file_exists($this->copyDestination)) $exists = count(array_diff(scandir($this->copyDestination), [".", ".."])) > 0;
        else $exists = false;
        return $exists;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getDownloadUrl(): string {
        return $this->downloadUrl;
    }

    public function getFileLocation(): string {
        return $this->fileLocation;
    }

    public function getUnzipLocation(): string {
        return $this->unzipLocation;
    }

    public function getClassLoadFolder(): ?string {
        return $this->classLoadFolder;
    }

    public function getClassLoadPath(): ?string {
        return $this->classLoadPath;
    }

    public function getExcludedFiles(): array {
        return $this->excludedFiles;
    }

    public function getCopySource(): string {
        return $this->copySource;
    }

    public function getCopyDestination(): string {
        return $this->copyDestination;
    }

    public function getDeletionDir(): ?string {
        return $this->deletionDir;
    }

    public function isCloudBridgeOnly(): bool {
        return $this->cloudBridgeOnly;
    }

    public function canBeLoaded(): bool {
        return !$this->cloudBridgeOnly && ($this->classLoadFolder !== null && $this->classLoadPath !== null);
    }
}