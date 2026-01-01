<?php

namespace pocketcloud\cloud\software;

use Closure;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\Utils;
use ReflectionException;
use const pocketcloud\SOFTWARE_PATH;

final readonly class ServerSoftware {

    /**
     * @param string $name
     * @param string $startCommand
     * @param string $url
     * @param string $fileName
     * @param array $aliases
     * @param Closure(ServerSoftware $software): Promise<bool> $checkForUpdateClosure
     * @param Closure(ServerSoftware $software): Promise<bool> $updateClosure
     * @throws ReflectionException
     */
    public function __construct(
        private string $name,
        private string $startCommand,
        private string $url,
        private string $fileName,
        private array $aliases,
        private Closure $checkForUpdateClosure,
        private Closure $updateClosure
    ) {
        Utils::validateCallbackSignature($this->checkForUpdateClosure, [ServerSoftware::class], Promise::class);
        Utils::validateCallbackSignature($this->updateClosure, [ServerSoftware::class], Promise::class);
    }

    /** @return Promise<bool> */
    public function checkForUpdate(): Promise {
        if (!@file_exists($this->getPath())) return Promise::resolved(true);
        return ($this->checkForUpdateClosure)($this);
    }

    public function update(): Promise {
        return ($this->updateClosure)($this);
    }

    public function getFileSize(): ?int {
        if (file_exists($this->getPath())) return filesize($this->getPath());
        return null;
    }

    public function getPath(): string {
        return SOFTWARE_PATH . $this->fileName;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getStartCommand(): string {
        return $this->startCommand;
    }

    public function getUrl(): string {
        return $this->url;
    }

    public function getFileName(): string {
        return $this->fileName;
    }

    public function getAliases(): array {
        return $this->aliases;
    }

    public function getCheckForUpdateClosure(): Closure {
        return $this->checkForUpdateClosure;
    }

    public function getUpdateClosure(): Closure {
        return $this->updateClosure;
    }
}