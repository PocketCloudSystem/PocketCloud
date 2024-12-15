<?php

namespace pocketcloud\cloud\config\type;

use pocketcloud\cloud\util\enum\EnumTrait;

/**
 * @method static ConfigType JSON()
 * @method static ConfigType YAML()
 * @method static ConfigType PROPERTIES()
 */
final class ConfigTypes {
    use EnumTrait;

    protected static function init(): void {
        self::register("json", new JsonConfigType());
        self::register("yaml", new YamlConfigType());
        self::register("properties", new PropertiesConfigType());
    }
}