<?php

namespace pocketcloud\cloud\template;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\server\config\ServerProperties;
use pocketcloud\cloud\server\config\ServerPropertiesGenerator;
use pocketcloud\cloud\software\Software;
use pocketcloud\cloud\software\SoftwareManager;
use pocketcloud\cloud\util\trait\RegistryTrait;
use const pocketcloud\GLOBAL_TEMPLATES_PATH;

/**
 * @method static TemplateType SERVER()
 * @method static TemplateType PROXY()
 */
final class TemplateType {
    use RegistryTrait;

    protected static function init(): void {
        self::add(new TemplateType("server", SoftwareManager::getInstance()->get("PocketMine-MP")));
        self::add(new TemplateType("proxy", SoftwareManager::getInstance()->get("WaterdogPE")));
    }

    public static function add(TemplateType $type): void {
        self::register(mb_strtoupper($type->getName()), $type);
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

    public function getName(): string {
        return $this->name;
    }

    public function getGlobalTemplatePath(): string {
        return GLOBAL_TEMPLATES_PATH . strtolower($this->name) . DIRECTORY_SEPARATOR;
    }

    public function getServerTimeout(): int {
        return MainConfig::getInstance()->getServerTimeout($this->name);
    }

    public function getServerPortRange(): array {
        return MainConfig::getInstance()->getServerPortRange($this->name);
    }

    public function getSoftware(): Software {
        return $this->software;
    }

    public function isServer(): bool {
        return $this->equals(self::SERVER());
    }

    public function isProxy(): bool {
        return $this->equals(self::PROXY());
    }

    /** @return array<ServerProperties> */
    public function getAssignedProperties(): array {
        return ServerPropertiesGenerator::getInstance()->getAll($this);
    }

    public function equals(TemplateType $type): bool {
        return $this->name === $type->getName();
    }

    public function __toString(): string {
        return $this->name;
    }
}