<?php

namespace pocketcloud\cloud\config;

use Closure;
use InvalidArgumentException;
use pocketcloud\cloud\config\type\ConfigType;
use pocketcloud\cloud\config\type\ConfigTypeList;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\exception\InvalidConfigTypeException;

final class Config {

    private ConfigType $type;
    private array $content = [];
    private bool $changed = false;

    public function __construct(
        private readonly string $path,
        ?ConfigType $type = null,
        private readonly array $defaultContent = []
    ) {
        if (!@file_exists(dirname($this->path))) throw new InvalidArgumentException("The given file path doesn't exists");
        if ($type === null) $type = ConfigTypeList::detectType($this->path);
        if ($type === null) throw new InvalidConfigTypeException("No config type specified, auto-detection found none");
        $this->type = $type;

        $this->load();
    }

    public function load(): void {
        if (!@file_exists($this->path)) {
            $this->content = $this->defaultContent;
            $this->changed = true;
            $this->save();
        }

        $fileContent = file_get_contents($this->path);
        if (!$fileContent) return;
        ExceptionHandler::attempt(function () use($fileContent): void {
            $this->content = $this->type->decodeContent($fileContent);
        });
    }

    public function reload(): void {
        $this->content = [];
        $this->changed = false;
        $this->load();
    }

    /**
     * @param Closure(string $filePath, array $content, ConfigType $type): bool|null $customSaveHandler
     * @return bool
     */
    public function save(?Closure $customSaveHandler = null): bool {
        if (!$this->changed) return true;
        $this->changed = false;
        if ($customSaveHandler !== null) {
            return ExceptionHandler::attempt(function (Closure $customSaveHandler): bool {
                return ($customSaveHandler)($this->path, $this->content, $this->type);
            }, "Failed to save configuration using custom handler", fn() => $this->changed = true, $customSaveHandler);
        }

        return ExceptionHandler::attempt(function (): bool {
            $rawContent = $this->type->encodeContent($this->content);
            return is_int(file_put_contents($this->path, $rawContent));
        }, "Failed to save configuration", fn() => $this->changed = true);
    }

    public function set(string $key, mixed $value): void {
        $keys = explode(".", $key);
        $currentRef = &$this->content;
        foreach ($keys as $subKey) {
            if (!isset($currentRef[$subKey]) || !is_array($currentRef[$subKey])) $currentRef[$subKey] = [];
            $currentRef = &$currentRef[$subKey];
        }

        $currentRef = $value;
        $this->changed = true;
    }

    public function setAll(array $content): void {
        $this->content = $content;
        $this->changed = true;
    }

    public function remove(string $key): void {
        $keys = explode(".", $key);
        $currentValue = &$this->content;
        foreach ($keys as $i => $subKey) {
            if (!isset($currentValue[$subKey])) return;
            if ($i == (count($keys) - 1)) {
                unset($currentValue[$subKey]);
                $this->changed = true;
                return;
            }

            $currentValue = &$currentValue[$subKey];
        }
    }

    public function clear(): void {
        $this->content = [];
        $this->changed = true;
    }

    public function has(string $key): bool {
        $keys = explode(".", $key);
        $currentValue = &$this->content;
        foreach ($keys as $subKey) {
            if (!isset($currentValue[$subKey])) return false;
            $currentValue = &$currentValue[$subKey];
        }

        return true;
    }

    public function get(string $key, mixed $default = null): mixed {
        $keys = explode(".", $key);
        $currentValue = $this->content;
        foreach ($keys as $subKey) {
            if (!isset($currentValue[$subKey])) return $default;
            $currentValue = $currentValue[$subKey];
        }

        return $currentValue;
    }

    public function getAll(bool $keys = false): array {
        return $keys ? array_keys($this->content) : $this->content;
    }

    public function hasChanged(): bool {
        return $this->changed;
    }
}