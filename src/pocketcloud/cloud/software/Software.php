<?php

namespace pocketcloud\cloud\software;

readonly class Software {

    public function __construct(
        private string $name,
        private string $startCommand,
        private string $url,
        private string $fileName,
        private array $aliases
    ) {}

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
}