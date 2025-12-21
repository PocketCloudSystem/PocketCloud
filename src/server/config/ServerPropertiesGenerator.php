<?php

namespace pocketcloud\cloud\server\config;

use pocketcloud\cloud\exception\PropertiesGenerateException;
use pocketcloud\cloud\server\config\def\PocketMineConfig;
use pocketcloud\cloud\server\config\def\PocketMineServerProperties;
use pocketcloud\cloud\server\config\def\WaterdogConfig;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class ServerPropertiesGenerator implements Loadable {
    use SingletonTrait;

    /** @var array<array<>> */
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

        if ($properties->needsRenewal($properties->getTemplateType()->getGlobalTemplatePath() . $properties->getFileName())) {
            $properties->renew($properties->getTemplateType()->getGlobalTemplatePath() . $properties->getFileName());
        }
    }

    public function remove(ServerProperties $properties): void {
        if (isset($this->defaultConfigFiles[$properties->getTemplateType()->getName()])) {
            unset($this->defaultConfigFiles[$properties->getTemplateType()->getName()]);
        }
    }
}