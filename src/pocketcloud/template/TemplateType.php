<?php

namespace pocketcloud\template;

use pocketcloud\software\Software;
use pocketcloud\software\SoftwareManager;
use pocketcloud\utils\EnumTrait;

/**
 * @method static TemplateType SERVER()
 * @method static TemplateType PROXY()
 */

final class TemplateType {
    use EnumTrait;

    protected static function init(): void {
        self::register("server", new TemplateType("SERVER", SoftwareManager::getInstance()->getSoftwareByName("PocketMine-MP")));
        self::register("proxy", new TemplateType("PROXY", SoftwareManager::getInstance()->getSoftwareByName("WaterdogPE")));
    }

    public static function getTemplateTypeByName(string $name): ?TemplateType {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    public static function getTemplateTypes(): array {
        self::check();
        return self::$members;
    }

    public function __construct(private string $name, private Software $software) {}

    public function __toString(): string {
        return $this->name;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getSoftware(): Software {
        return $this->software;
    }
}