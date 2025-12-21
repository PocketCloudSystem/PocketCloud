<?php

namespace pocketcloud\cloud\util\loader;

use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;

final class ClassLoader extends ThreadSafe implements IClassLoader {

    private ThreadSafeArray $namespaces;

    public function __construct() {
        $this->namespaces = new ThreadSafeArray();
    }

    public function init(): void {
        spl_autoload_register($this->loadClass(...));
    }

    public function addPrefix(string $namespace, string $path): void {
        $this->namespaces->synchronized(function (string $namespace, string $path): void {
            $namespace = str_replace(["/", "//", "\\", "\\\\"], DIRECTORY_SEPARATOR, rtrim($namespace, "\\")) . DIRECTORY_SEPARATOR;
            if (!isset($this->namespaces[$namespace])) $this->namespaces[$namespace] = new ThreadSafeArray();
            $this->namespaces[$namespace][] = $path;
        }, $namespace, $path);
    }

    public function findClass(string $class): ?string {
        return $this->synchronized(function (string $class): ?string {
            $class = str_replace(["/", "//", "\\", "\\\\"], DIRECTORY_SEPARATOR, rtrim($class, "\\"));

            foreach ($this->namespaces as $prefix => $paths) {
                if (str_starts_with($class, $prefix)) {
                    $relative = substr($class, strlen($prefix)) . ".php";
                    $secondRelative = $class . ".php";
                    foreach ($paths as $path) {
                        $file = rtrim($path, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . $relative;
                        $secondFile = rtrim($path, DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR . $secondRelative;
                        if (file_exists($file)) return $file;
                        if (file_exists($secondFile)) return $secondFile;
                    }
                }
            }

            return null;
        }, $class);
    }

    public function loadClass(string $class): bool {
        if (($path = $this->findClass($class)) !== null) {
            include_once $path;
            if (!class_exists($class, false) && !trait_exists($class, false) && !interface_exists($class, false)) {
                return false;
            }

            return true;
        }
        return false;
    }
}