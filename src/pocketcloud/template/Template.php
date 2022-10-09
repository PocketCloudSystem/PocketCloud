<?php

namespace pocketcloud\template;

use pocketcloud\utils\Utils;

class Template {

    public function __construct(private string $name, private bool $lobby, private bool $maintenance, private int $maxPlayerCount, private int $minServerCount, private int $maxServerCount, private bool $autoStart, private TemplateType $templateType) {}

    public function getName(): string {
        return $this->name;
    }

    public function isLobby(): bool {
        return $this->lobby;
    }

    public function isMaintenance(): bool {
        return $this->maintenance;
    }

    public function getMaxPlayerCount(): int {
        return $this->maxPlayerCount;
    }

    public function getMinServerCount(): int {
        return $this->minServerCount;
    }

    public function getMaxServerCount(): int {
        return $this->maxServerCount;
    }

    public function isAutoStart(): bool {
        return $this->autoStart;
    }

    public function getTemplateType(): TemplateType {
        return $this->templateType;
    }

    public function setLobby(bool $value): void {
        $this->lobby = $value;
    }

    public function setMaintenance(bool $value): void {
        $this->maintenance = $value;
    }

    public function setMaxPlayerCount(int $maxPlayerCount): void {
        $this->maxPlayerCount = $maxPlayerCount;
    }

    public function setMinServerCount(int $minServerCount): void {
        $this->minServerCount = $minServerCount;
    }

    public function setMaxServerCount(int $maxServerCount): void {
        $this->maxServerCount = $maxServerCount;
    }

    public function setAutoStart(bool $autoStart): void {
        $this->autoStart = $autoStart;
    }

    public function getPath(): string {
        return TEMPLATES_PATH . $this->name . "/";
    }

    public function toArray(): array {
        return [
            "name" => $this->name,
            "lobby" => $this->lobby,
            "maintenance" => $this->maintenance,
            "maxPlayerCount" => $this->maxPlayerCount,
            "minServerCount" => $this->minServerCount,
            "maxServerCount" => $this->maxServerCount,
            "autoStart" => $this->autoStart,
            "templateType" => $this->templateType->getName()
        ];
    }

    public static function fromArray(array $template): ?Template {
        if (!Utils::containKeys($template, "name", "lobby", "maintenance", "maxPlayerCount", "minServerCount", "maxServerCount", "autoStart", "templateType")) return null;
        return new Template(
            $template["name"],
            boolval($template["lobby"]),
            boolval($template["maintenance"]),
            intval($template["maxPlayerCount"]),
            intval($template["minServerCount"]),
            intval($template["maxServerCount"]),
            boolval($template["autoStart"]),
            TemplateType::getTemplateTypeByName($template["templateType"]) ?? TemplateType::SERVER()
        );
    }

    public static function isValidEditValue(string $value, string $key, ?string &$expected = null, mixed &$realValue = null): bool {
        if ($key == "lobby" || $key == "maintenance" || $key == "autoStart") {
            $expected = "true | false";
            if ($value == "true" || $value == "false") {
                $realValue = $value == "true";
                return true;
            }
        } else if ($key == "maxPlayerCount" || $key == "minServerCount" || $key == "maxServerCount") {
            $expected = "number";
            if (is_numeric($value)) {
                $realValue = (max(intval($value), 0));
                return true;
            }
        }
        return false;
    }

    public static function isValidEditKey(string $key): bool {
        return $key == "lobby" || $key == "maintenance" || $key == "maxPlayerCount" || $key == "minServerCount" || $key == "maxServerCount" || $key == "autoStart";
    }
}