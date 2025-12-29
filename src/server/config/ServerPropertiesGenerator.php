<?php

namespace pocketcloud\cloud\server\config;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\exception\PropertiesGenerateException;
use pocketcloud\cloud\server\config\def\PocketMineConfig;
use pocketcloud\cloud\server\config\def\PocketMineServerProperties;
use pocketcloud\cloud\server\config\def\WaterdogConfig;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class ServerPropertiesGenerator implements Loadable {
    use SingletonTrait;

    /** @var array<array<ServerProperties>> */
    private array $defaultConfigFiles = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {
        $this->register(new PocketMineConfig());
        $this->register(new PocketMineServerProperties());
        $this->register(new WaterdogConfig());
    }

    public function register(ServerProperties $properties): void {
        if (!isset($this->defaultConfigFiles[$properties->getTemplateType()->getName()])) $this->defaultConfigFiles[$properties->getTemplateType()->getName()] = [];
        $this->defaultConfigFiles[$properties->getTemplateType()->getName()][] = $properties;

        if (!file_exists($properties->getTemplateType()->getGlobalTemplatePath())) {
            throw new PropertiesGenerateException("The global template path for template type '" . $properties->getTemplateType()->getName() . "' does not exist");
        }

        if ($properties->needsRenewal($propertiesPath = $properties->getTemplateType()->getGlobalTemplatePath() . $properties->getFileName()) || !@file_exists($propertiesPath)) {
            CloudLogger::get()->info("§aUpdating §rserver properties/config§8: §b{}§8...", $properties->getFileName());
            $properties->renew($propertiesPath);
        }
    }

    public function remove(ServerProperties $properties): void {
        if (isset($this->defaultConfigFiles[$properties->getTemplateType()->getName()])) {
            unset($this->defaultConfigFiles[$properties->getTemplateType()->getName()]);
        }
    }

    public function getAll(?TemplateType $type): ?array {
        if ($type === null) return $this->defaultConfigFiles;
        if (!isset($this->defaultConfigFiles[$type->getName()])) return null;
        return $this->defaultConfigFiles[$type->getName()];
    }
}