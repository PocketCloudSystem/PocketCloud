<?php

namespace pocketcloud\lib\config;

use pocketcloud\utils\CloudLogger;
use ReflectionClass;

class Configuration {

    public const KNOWN_TYPES = [
        Configuration::TYPE_YAML, Configuration::TYPE_JSON
    ];

    public const TYPE_YAML = 0;
    public const TYPE_JSON = 1;

    public function __construct(private string $path, private int $type) {
        if (!in_array($this->getType(), self::KNOWN_TYPES)) {
            throw new \InvalidArgumentException("Unknown configuration type");
        }
    }

    /**
     * @throws \JsonException
     */
    public function load(): bool {
        if (file_exists($this->getPath()) && is_file($this->getPath())) {
            $contents = file_get_contents($this->getPath());
            $contents = ($this->getType() == Configuration::TYPE_YAML ? yaml_parse($contents) : json_decode($contents, true, flags: JSON_THROW_ON_ERROR));

            foreach ((new ReflectionClass($this))->getProperties() as $property) {
                if (!str_contains($property->getDocComment(), "@ignored")) {
                    $property->setAccessible(true);
                    if (array_key_exists($property->getName(), $contents)) {
                        $property->setValue($this, $contents[$property->getName()]);
                    } else {
                        CloudLogger::get()->debug("Can't find key '" . $property->getName() . "' in config: " . $this->getPath());
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @throws \JsonException
     */
    public function save(): bool {
        $contents = [];
        foreach ((new ReflectionClass($this))->getProperties() as $property) {
            if (!str_contains($property->getDocComment(), "@ignored")) {
                $property->setAccessible(true);
                $contents[$property->getName()] = $property->getValue($this);
            }
        }

        $contents = ($this->getType() == Configuration::TYPE_YAML ? yaml_emit($contents, YAML_UTF8_ENCODING) : json_encode($contents, JSON_PRETTY_PRINT | JSON_BIGINT_AS_STRING | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR));
        return file_put_contents($this->getPath(), $contents) !== false;
    }

    public function getPath(): string {
        return $this->path;
    }

    public function getType(): int {
        return $this->type;
    }
}