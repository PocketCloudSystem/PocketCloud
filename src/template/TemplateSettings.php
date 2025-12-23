<?php

namespace pocketcloud\cloud\template;

use pocketcloud\cloud\util\Utils;

final class TemplateSettings {

    public function __construct(
        private bool $lobby,
        private bool $maintenance,
        private bool $static,
        private bool $alwaysCopyToStaticServers,
        private int $maxPlayerCount,
        private int $minServerCount,
        private int $maxServerCount,
        private float $startNewPercentage,
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

    public function setAlwaysCopyToStaticServers(bool $alwaysCopyToStaticServers): void {
        $this->alwaysCopyToStaticServers = $alwaysCopyToStaticServers;
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

    public function setStartNewPercentage(float $startNewPercentage): void {
        $this->startNewPercentage = $startNewPercentage;
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

    public function isAlwaysCopyToStaticServers(): bool {
        return $this->alwaysCopyToStaticServers;
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

    public function getStartNewPercentage(): float {
        return $this->startNewPercentage;
    }

    public function isAutoStart(): bool {
        return $this->autoStart;
    }

    public function write(): array {
        return [
            "lobby" => $this->lobby,
            "maintenance" => $this->maintenance,
            "static" => $this->static,
            "alwaysCopyToStaticServers" => $this->alwaysCopyToStaticServers,
            "maxPlayerCount" => $this->maxPlayerCount,
            "minServerCount" => $this->minServerCount,
            "maxServerCount" => $this->maxServerCount,
            "startNewPercentage" => $this->startNewPercentage,
            "autoStart" => $this->autoStart
        ];
    }

    public static function create(bool $lobby, bool $maintenance, bool $static, bool $alwaysCopyToStaticServers, int $maxPlayerCount, int $minServerCount, int $maxServerCount, float $startNewPercentage, bool $autoStart): self {
        return new TemplateSettings($lobby, $maintenance, $static, $alwaysCopyToStaticServers, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewPercentage, $autoStart);
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, "lobby", "maintenance", "static", "alwaysCopyToStaticServers", "maxPlayerCount", "minServerCount", "maxServerCount", "startNewPercentage", "autoStart")) return null;
        return self::create(...$data);
    }
}