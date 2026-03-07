<?php

namespace pocketcloud\cloud\template;

use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\Utils;
use const pocketcloud\TEMPLATES_PATH;

final readonly class Template implements Writeable {

    public function __construct(
        private string $name,
        private TemplateSettings $templateSettings,
        private TemplateType $templateType
    ) {}

    public function setLobby(bool $lobby): self {
        $this->templateSettings->setLobby($lobby);
        return $this;
    }

    public function setMaintenance(bool $maintenance): self {
        $this->templateSettings->setMaintenance($maintenance);
        return $this;
    }

    public function setStatic(bool $static): self {
        $this->templateSettings->setStatic($static);
        return $this;
    }

    public function setAlwaysCopyToStaticServers(bool $alwaysCopyToStaticServers): self {
        $this->templateSettings->setAlwaysCopyToStaticServers($alwaysCopyToStaticServers);
        return $this;
    }

    public function setMaxPlayerCount(int $maxPlayerCount): self {
        $this->templateSettings->setMaxPlayerCount($maxPlayerCount);
        return $this;
    }

    public function setMinServerCount(int $minServerCount): self {
        $this->templateSettings->setMinServerCount($minServerCount);
        return $this;
    }

    public function setMaxServerCount(int $maxServerCount): self {
        $this->templateSettings->setMaxServerCount($maxServerCount);
        return $this;
    }

    public function setStartNewPercentage(float $startNewPercentage): self {
        $this->templateSettings->setStartNewPercentage($startNewPercentage);
        return $this;
    }

    public function setAutoStart(bool $autoStart): self {
        $this->templateSettings->setAutoStart($autoStart);
        return $this;
    }

    /** @return array<CloudPlayer> */
    public function getPlayers(): array {
        return array_filter(CloudPlayerManager::getInstance()->getAll(), fn(CloudPlayer $player) => str_starts_with($player->getCurrentProxyName(), $this->getName()) || str_starts_with($player->getCurrentServerName(), $this->getName()));
    }

    public function getPlayerCount(): int {
        return count($this->getPlayers());
    }

    public function getName(): string {
        return $this->name;
    }

    public function isLobby(): bool {
        return $this->templateSettings->isLobby();
    }

    public function isMaintenance(): bool {
        return $this->templateSettings->isMaintenance();
    }

    public function isStatic(): bool {
        return $this->templateSettings->isStatic();
    }

    public function isAlwaysCopyToStaticServers(): bool {
        return $this->templateSettings->isAlwaysCopyToStaticServers();
    }

    public function getMaxPlayerCount(): int {
        return $this->templateSettings->getMaxPlayerCount();
    }

    public function getMinServerCount(): int {
        return $this->templateSettings->getMinServerCount();
    }

    public function getMaxServerCount(): int {
        return $this->templateSettings->getMaxServerCount();
    }

    public function getStartNewPercentage(): float {
        return $this->templateSettings->getStartNewPercentage();
    }

    public function isAutoStart(): bool {
        return $this->templateSettings->isAutoStart();
    }

    public function getSettings(): TemplateSettings {
        return $this->templateSettings;
    }

    public function getTemplateType(): TemplateType {
        return $this->templateType;
    }

    public function getParentServerGroup(): ?ServerGroup {
        return ServerGroupManager::getInstance()->get($this);
    }

    public function getPath(): string {
        return PathUtils::join(TEMPLATES_PATH, $this->name) . "/";
    }

    public function write(): array {
        $playerCount = 0;
        $serverCount = count(CloudServerManager::getInstance()->getAll($this));
        foreach (CloudServerManager::getInstance()->getAll($this) as $server) $playerCount += $server->getPlayerCount();
        return [
            "name" => $this->name,
            "lobby" => $this->isLobby(),
            "maintenance" => $this->isMaintenance(),
            "static" => $this->isStatic(),
            "players" => $playerCount,
            "servers" => $serverCount,
            "alwaysCopyToStaticServers" => $this->isMaintenance(),
            "maxPlayerCount" => $this->getMaxPlayerCount(),
            "minServerCount" => $this->getMinServerCount(),
            "maxServerCount" => $this->getMaxServerCount(),
            "startNewPercentage" => $this->getStartNewPercentage(),
            "autoStart" => $this->isAutoStart(),
            "templateType" => $this->templateType->getName()
        ];
    }

    public static function create(string $name, TemplateSettings $templateSettings, TemplateType $templateType): self {
        return new Template($name, $templateSettings, $templateType);
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, ...TemplateHelper::NECESSARY_KEYS)) return null;
        TemplateHelper::fillKeys($data);
        return self::create($data["name"], TemplateHelper::sumSettingsToInstance($data), TemplateType::get($data["templateType"]) ?? TemplateType::SERVER());
    }

    public static function server(string $name, bool $lobby = false, bool $maintenance = true, bool $static = false, bool $alwaysCopyToStaticServers = false, int $maxPlayerCount = 20, int $minServerCount = 1, int $maxServerCount = 2, float $startNewPercentage = 100, bool $autoStart = true): self {
        return self::create($name, TemplateSettings::create($lobby, $maintenance, $static, $alwaysCopyToStaticServers, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewPercentage, $autoStart), TemplateType::SERVER());
    }

    public static function proxy(string $name, bool $maintenance = true, bool $static = false, bool $alwaysCopyToStaticServers = false, int $maxPlayerCount = 20, int $minServerCount = 1, int $maxServerCount = 1, float $startNewPercentage = 0, bool $autoStart = true): self {
        return self::create($name, TemplateSettings::create(false, $maintenance, $static, $alwaysCopyToStaticServers, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewPercentage, $autoStart), TemplateType::PROXY());
    }

    public static function lobby(string $name, bool $maintenance = true, bool $static = false, bool $alwaysCopyToStaticServers = false, int $maxPlayerCount = 20, int $minServerCount = 1, int $maxServerCount = 2, float $startNewPercentage = 100, bool $autoStart = true): self {
        return self::create($name, TemplateSettings::create(true, $maintenance, $static, $alwaysCopyToStaticServers, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewPercentage, $autoStart), TemplateType::SERVER());
    }
}