<?php

namespace pocketcloud\util;

use JetBrains\PhpStorm\Pure;
use Phar;
use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;

final class ClassLoader extends ThreadSafe {

    private ThreadSafeArray $paths;

    public function __construct() {
        $this->paths = ThreadSafeArray::fromArray([]);
    }

    public function init(): void {
        $this->addPath("pocketcloud\\", SOURCE_PATH);
        spl_autoload_register(function (string $class): void {
            if (($path = $this->getFullPath(self::validate($class))) !== null) {
                if (file_exists($path) || (IS_PHAR && file_exists($path = Phar::running() . "/src/" . self::validate($class) . ".php")) && !class_exists($class)) {
                    require_once $path;
                }
            }
        });
    }

    public function addPath(string $folder, string $path): void {
        if (!isset($this->paths[self::validate($folder)])) $this->paths[self::validate($folder)] = ThreadSafeArray::fromArray([]);
        $this->paths[self::validate($folder)][] = self::validate(self::addSeparator($path));
    }

    public function getFullPath(string $class): ?string {
        foreach ($this->paths as $src => $p) {
            foreach ($p as $key) {
                if (!$src) {
                    if (file_exists(($path = $key . $class . ".php"))) return $path;
                    continue;
                }

                if (str_contains($class, $src)) return self::replaceLast($src, $class . ".php", $key);
            }
        }
        return null;
    }

    #[Pure] private static function addSeparator(string $path): string {
        return (str_ends_with($path, DIRECTORY_SEPARATOR) ? $path : $path . DIRECTORY_SEPARATOR);
    }

    #[Pure] public static function replaceLast(string $search, string $replace, string $subject): string {
        if(($pos = strrpos($subject, $search)) !== false) {
            $search_length = strlen($search);
            $subject = substr_replace($subject, $replace, $pos, $search_length);
        }
        return $subject;
    }

    private static function validate(string $folder): string {
        return str_replace(["//", "\\", "\\\\"], DIRECTORY_SEPARATOR, $folder);
    }
}