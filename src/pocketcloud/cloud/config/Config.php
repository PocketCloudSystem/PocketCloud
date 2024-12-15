<?php

namespace pocketcloud\cloud\config;

use InvalidArgumentException;
use pocketcloud\cloud\config\type\ConfigType;
use pocketcloud\cloud\exception\ExceptionHandler;

final class Config {

    private array $content = [];
    private bool $changed = false;

    public function __construct(
        private readonly string $file,
        private readonly ConfigType $configType
    ) {
        $this->load();
    }

    public function load(): void {
        if (!@file_exists($this->file)) $this->save();

        $fileContent = file_get_contents($this->file);
        if (!$fileContent) return;
        ExceptionHandler::tryCatch(function () use($fileContent): void {
            $this->content = $this->configType->decodeContent($fileContent);
        });
    }

    public function save(): void {
        if (!@file_exists(dirname($this->file . DIRECTORY_SEPARATOR))) {
            ExceptionHandler::tryCatch(fn() => throw new InvalidArgumentException("The given file path doesn't exists"));
            return;
        }

        ExceptionHandler::tryCatch(function (): void {
            $rawContent = $this->configType->encodeContent($this->content);
            file_put_contents($this->file, $rawContent);
        });
    }

    public function reload(): void {
        $this->content = [];
        $this->changed = false;
        $this->load();
    }

    public function set(string $key, mixed $value): void {
        $this->content[$key] = $value;
        $this->changed = true;
    }

    public function setNested(string $key, mixed $value): void {
        $keys = explode(".", $key);
        $content =& $this->content;
        while (count($keys) > 0) {
            $currentKey = array_shift($keys);
            if (!isset($content[$currentKey])) $content[$currentKey] = [];
            $content =& $content[$currentKey];
        }

        $content = $value;
        $this->changed = true;
    }

    public function setAll(array $content): void {
        $this->content = $content;
        $this->changed = true;
    }

    public function remove(string $key): void {
        if (isset($this->content[$key])) {
            unset($this->content[$key]);
            $this->changed = true;
        }
    }

    public function removeNested(string $key): void {
        if (isset($this->content[$key])) {
            unset($this->content[$key]);
            return;
        }

        $keys = explode(".", $key);
        $content =& $this->content;
        while (count($keys) > 0) {
            $currentKey = array_shift($keys);
            if (!isset($content[$currentKey])) break;
            if (is_array($content[$currentKey])) $content =& $content[$currentKey];
            else if (count($keys) == 0) unset($content[$currentKey]);
        }

        $this->changed = true;
    }

    public function has(string $key): bool {
        return isset($this->content[$key]);
    }

    public function hasNested(string $key): bool {
        if (isset($this->content[$key])) return true;
        $keys = explode(".", $key);
        $content =& $this->content;
        while (count($keys) > 0) {
            $currentKey = array_shift($keys);
            if (!isset($content[$currentKey])) return false;
            if (is_array($content[$currentKey])) $content =& $content[$currentKey];
            else if (count($keys) == 0) return true;
        }

        return false;
    }

    public function get(string $key, mixed $default = null): mixed {
        return $this->content[$key] ?? $default;
    }

    public function getNested(string $key, mixed $default = null): mixed {
        if ($this->has($key)) return $this->get($key, $default);
        $keys = explode(".", $key);
        $content =& $this->content;
        while (count($keys) > 0) {
            $currentKey = array_shift($keys);
            if (!isset($content[$currentKey])) return $default;
            if (is_array($content[$currentKey])) $content =& $content[$currentKey];
            else if (count($keys) == 0) return $content[$currentKey];
        }

        return $default;
    }

    public function getAll(bool $keysOnly = false): array {
        return $keysOnly ? array_keys($this->content) : $this->content;
    }

    public function hasChanged(): bool {
        return $this->changed;
    }
}