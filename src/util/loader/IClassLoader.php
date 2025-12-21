<?php

namespace pocketcloud\cloud\util\loader;

interface IClassLoader {

    public function init(): void;

    public function addPrefix(string $namespace, string $path): void;

    public function findClass(string $class): ?string;

    public function loadClass(string $class): bool;
}