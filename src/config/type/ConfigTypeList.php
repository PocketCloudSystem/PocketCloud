<?php

namespace pocketcloud\cloud\config\type;

use pocketcloud\cloud\util\trait\RegistryTrait;

/**
 * @method static ConfigType JSON();
 * @method static ConfigType YAML();
 * @method static ConfigType YML();
 * @method static ConfigType PROPERTIES();
 */
final class ConfigTypeList {
    use RegistryTrait {
        register as public;
    }

    protected static function init(): void {
        self::register("json", new JsonConfigType());
        self::register("yaml", new YamlConfigType());
        self::register("yml", new YamlConfigType());
        self::register("properties", new PropertiesConfigType());
    }

    public static function detectType(string $filePath): ?ConfigType {
        self::check();
        $extension = pathinfo($filePath, PATHINFO_EXTENSION);
        if (!is_string($extension)) return null;
        return self::$members[mb_strtoupper($extension)] ?? null;
    }
}