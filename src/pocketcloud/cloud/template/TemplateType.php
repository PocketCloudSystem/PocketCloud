<?php

namespace pocketcloud\cloud\template;

use pocketcloud\cloud\software\Software;
use pocketcloud\cloud\software\SoftwareManager;
use pocketcloud\cloud\util\enum\EnumTrait;

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

    public static function get(string $name): ?TemplateType {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    /** @return array<TemplateType> */
    public static function getAll(): array {
        self::check();
        return self::$members;
    }

    public function __construct(
        private readonly string $name,
        private readonly Software $software
    ) {}

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