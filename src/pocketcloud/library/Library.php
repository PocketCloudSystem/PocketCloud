<?php

namespace pocketcloud\library;

use pocketcloud\util\Utils;

class Library {

    public function __construct(
        private readonly string $name,
        private readonly string $downloadUrl,
        private readonly string $fileLocation,
        private readonly string $unzipLocation,
        private readonly array $excludedFiles = [],
        private readonly string $copySource = "",
        private readonly string $copyDestination = "",
        private readonly ?string $deletionDir = null,
        private readonly bool $cloudBridgeOnly = false
    ) {}

    public function download(): bool {
        if (!Utils::download($this->downloadUrl, $this->fileLocation)) return false;
        $archive = new \ZipArchive();
        if ($archive->open($this->fileLocation)) {
            if (!file_exists($this->unzipLocation)) mkdir($this->unzipLocation);
            $archive->extractTo($this->unzipLocation);
            if ($this->copySource !== "" && $this->copyDestination !== "") {
                Utils::copyDir($this->copySource, $this->copyDestination);
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
        if ($this->copySource !== "") Utils::deleteDir($this->deletionDir ?? $this->copySource);
        return true;
    }

    public function exists(): bool {
        $exists = false;
        if (file_exists($this->unzipLocation)) $exists = count(array_diff(scandir($this->unzipLocation), [".", ".."])) > 0;
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

    public function getExcludedFiles(): array {
        return $this->excludedFiles;
    }

    public function getCopySource(): string {
        return $this->copySource;
    }

    public function getCopyDestination(): string {
        return $this->copyDestination;
    }

    public function isCloudBridgeOnly(): bool {
        return $this->cloudBridgeOnly;
    }
}