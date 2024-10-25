<?php

namespace pocketcloud\plugin;

use JetBrains\PhpStorm\ArrayShape;
use pocketcloud\util\Utils;

readonly class CloudPluginDescription {

    public function __construct(
        private string $name,
        private string $main,
        private string $version,
        private array $authors = [],
        private ?string $description = null
    ) {}

    public function getName(): string {
        return $this->name;
    }

    public function getMain(): string {
        return $this->main;
    }

    public function getVersion(): string {
        return $this->version;
    }

    public function getAuthors(): array {
        return $this->authors;
    }

    public function getDescription(): ?string {
        return $this->description;
    }

    public function getFullName(): string {
        return $this->name . "@v" . $this->version;
    }

    #[ArrayShape(["name" => "string", "main" => "string", "version" => "string", "authors" => "array", "description" => "null|string"])] public function toArray(): array {
        return [
            "name" => $this->name,
            "main" => $this->main,
            "version" => $this->version,
            "authors" => $this->authors,
            "description" => $this->description,
        ];
    }

    public static function fromArray(array $description): ?self {
        if (!Utils::containKeys($description, "name", "main", "version")) return null;
        return new CloudPluginDescription(
            $description["name"],
            $description["main"],
            (string)$description["version"],
            ($description["authors"] ?? (isset($description["author"]) ? [$description["author"]] : [])),
            $description["description"] ?? null
        );
    }
}