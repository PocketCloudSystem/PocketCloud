<?php

namespace pocketcloud\cloud\plugin;

use JetBrains\PhpStorm\ArrayShape;
use pocketcloud\cloud\util\Utils;

readonly class CloudPluginDescription {

    public function __construct(
        private string $name,
        private string $main,
        private string $version,
        private string $srcNamespacePrefix = "",
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

    public function getSrcNamespacePrefix(): string {
        return $this->srcNamespacePrefix;
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

    #[ArrayShape(["name" => "string", "main" => "string", "version" => "string", "srcNamespacePrefix" => "string", "authors" => "array", "description" => "null|string"])]
    public function write(): array {
        return [
            "name" => $this->name,
            "main" => $this->main,
            "version" => $this->version,
            "srcNamespacePrefix" => $this->srcNamespacePrefix,
            "authors" => $this->authors,
            "description" => $this->description,
        ];
    }

    public static function read(array $description): ?self {
        if (!Utils::containKeys($description, "name", "main", "version")) return null;
        return new CloudPluginDescription(
            $description["name"],
            $description["main"],
            (string) $description["version"],
            $description["src-namespace-prefix"] ?? "",
            ($description["authors"] ?? (isset($description["author"]) ? [$description["author"]] : [])),
            $description["description"] ?? null
        );
    }
}