<?php

namespace pocketcloud\cloud\server\config;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\server\config\def\PocketMineConfig;
use pocketcloud\cloud\server\config\def\PocketMineServerProperties;
use pocketcloud\cloud\server\config\def\WaterdogConfig;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\trait\SingletonTrait;
use const pocketcloud\TEMPLATES_PATH;

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

        if (!file_exists($properties->getTemplateType()->getGlobalTemplatePath())) mkdir($properties->getTemplateType()->getGlobalTemplatePath(), 0777, true);

        if ($properties->needsRenewal($propertiesPath = $properties->getTemplateType()->getGlobalTemplatePath() . $properties->getFileName()) || !file_exists($propertiesPath)) {
            CloudLogger::get()->info("§aUpdating §rserver properties/config§8: §b{}§8...", $properties->getFileName());
            $properties->renew($propertiesPath);

            $i = 0;
            foreach (array_diff(scandir(TEMPLATES_PATH), [".", "..", "global"]) as $file) {
                $dirPath = PathUtils::join(TEMPLATES_PATH, $file) . "/";
                $filePath = PathUtils::join($dirPath, $properties->getFileName());
                if (file_exists($filePath)) {
                    if ($properties->needsRenewal($filePath)) {
                        $properties->renew($filePath);
                        $i++;
                    }
                }
            }

            if ($i > 0) {
                CloudLogger::get()->info("Also §aupdating §rserver properties/config §rfor §b{} templates§r: §b{}§8...", $i, $properties->getFileName());
            }
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