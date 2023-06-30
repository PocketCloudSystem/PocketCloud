<?php

namespace pocketcloud\software;

class Software {

    public function __construct(
        private readonly string $name,
        private readonly string $startCommand,
        private readonly string $url,
        private readonly string $fileName,
        private readonly array $aliases
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