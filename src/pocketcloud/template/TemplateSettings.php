<?php

namespace pocketcloud\template;

use pocketcloud\util\Utils;

final class TemplateSettings {

    public function __construct(
        private bool $lobby,
        private bool $maintenance,
        private bool $static,
        private int $maxPlayerCount,
        private int $minServerCount,
        private int $maxServerCount,
        private bool $startNewWhenFull,
        private bool $autoStart
    ) {}

    public function setLobby(bool $lobby): void {
        $this->lobby = $lobby;
    }

    public function setMaintenance(bool $maintenance): void {
        $this->maintenance = $maintenance;
    }

    public function setStatic(bool $static): void {
        $this->static = $static;
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

    public function setStartNewWhenFull(bool $startNewWhenFull): void {
        $this->startNewWhenFull = $startNewWhenFull;
    }

    public function setAutoStart(bool $autoStart): void {
        $this->autoStart = $autoStart;
    }

    public function isLobby(): bool {
        return $this->lobby;
    }

    public function isMaintenance(): bool {
        return $this->maintenance;
    }

    public function isStatic(): bool {
        return $this->static;
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

    public function isStartNewWhenFull(): bool {
        return $this->startNewWhenFull;
    }

    public function isAutoStart(): bool {
        return $this->autoStart;
    }

    public function toArray(): array {
        return [
            "lobby" => $this->lobby,
            "maintenance" => $this->maintenance,
            "static" => $this->static,
            "maxPlayerCount" => $this->maxPlayerCount,
            "minServerCount" => $this->minServerCount,
            "maxServerCount" => $this->maxServerCount,
            "startNewWhenFull" => $this->startNewWhenFull,
            "autoStart" => $this->autoStart
        ];
    }

    public static function create(bool $lobby, bool $maintenance, bool $static, int $maxPlayerCount, int $minServerCount, int $maxServerCount, bool $startNewWhenFull, bool $autoStart): self {
        return new TemplateSettings($lobby, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewWhenFull, $autoStart);
    }

    public static function fromArray(array $data): ?self {
        if (!Utils::containKeys($data, "lobby", "maintenance", "maxPlayerCount", "minServerCount", "maxServerCount", "autoStart")) return null;
        return self::create(...$data);
    }
}