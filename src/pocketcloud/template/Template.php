<?php

namespace pocketcloud\template;

use pocketcloud\util\Utils;

class Template {

    public function __construct(
        private readonly string $name,
        private readonly TemplateSettings $templateSettings,
        private readonly TemplateType $templateType
    ) {}

    public function getName(): string {
        return $this->name;
    }

    public function getSettings(): TemplateSettings {
        return $this->templateSettings;
    }

    public function getTemplateType(): TemplateType {
        return $this->templateType;
    }

    public function getPath(): string {
        return TEMPLATES_PATH . $this->name . "/";
    }

    public function toArray(): array {
        return [
            "name" => $this->name,
            "lobby" => $this->templateSettings->isLobby(),
            "maintenance" => $this->templateSettings->isMaintenance(),
            "static" => $this->templateSettings->isStatic(),
            "maxPlayerCount" => $this->templateSettings->getMaxPlayerCount(),
            "minServerCount" => $this->templateSettings->getMinServerCount(),
            "maxServerCount" => $this->templateSettings->getMaxServerCount(),
            "startNewWhenFull" => $this->templateSettings->isStartNewWhenFull(),
            "autoStart" => $this->templateSettings->isAutoStart(),
            "templateType" => $this->templateType->getName()
        ];
    }

    public static function create(string $name, TemplateSettings $templateSettings, TemplateType $templateType): self {
        return new Template($name, $templateSettings, $templateType);
    }

    public static function fromArray(array $data): ?self {
        if (!Utils::containKeys($data, ...TemplateHelper::NECESSARY_KEYS)) return null;
        TemplateHelper::addUnnecessaryKeys($data);
        return self::create($data["name"], TemplateHelper::sumSettingsToInstance($data), TemplateType::get($data["templateType"]) ?? TemplateType::SERVER());
    }

    public static function server(string $name, bool $lobby = false, bool $maintenance = true, bool $static = false, int $maxPlayerCount = 20, int $minServerCount = 1, int $maxServerCount = 2, bool $startNewWhenFull = true, bool $autoStart = true): self {
        return self::create($name, TemplateSettings::create($lobby, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewWhenFull, $autoStart), TemplateType::SERVER());
    }

    public static function proxy(string $name, bool $maintenance = true, bool $static = false, int $maxPlayerCount = 20, int $minServerCount = 1, int $maxServerCount = 1, bool $startNewWhenFull = false, bool $autoStart = true): self {
        return self::create($name, TemplateSettings::create(false, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewWhenFull, $autoStart), TemplateType::PROXY());
    }

    public static function lobby(string $name, bool $maintenance = true, bool $static = false, int $maxPlayerCount = 20, int $minServerCount = 1, int $maxServerCount = 2, bool $startNewWhenFull = true, bool $autoStart = true): self {
        return self::create($name, TemplateSettings::create(true, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewWhenFull, $autoStart), TemplateType::SERVER());
    }
}