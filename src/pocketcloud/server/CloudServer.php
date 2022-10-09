<?php

namespace pocketcloud\server;

use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\normal\LocalPlayerRegisterPacket;
use pocketcloud\network\packet\impl\normal\LocalServerRegisterPacket;
use pocketcloud\network\packet\impl\normal\LocalServerUpdatePacket;
use pocketcloud\network\packet\impl\normal\LocalTemplateRegisterPacket;
use pocketcloud\network\packet\impl\normal\ProxyRegisterServerPacket;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\server\data\CloudServerData;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\utils\Utils;

class CloudServer {

    private float $startTime;
    private float $lastCheckTime;
    private bool $isAlive;
    private float $stopTime = 0.0;

    public function __construct(private int $id, private Template $template, private CloudServerData $cloudServerData, private ServerStatus $serverStatus) {
        $this->startTime = microtime(true);
    }

    public function getName(): string {
        return $this->template->getName() . "-" . $this->id;
    }

    public function getId(): int {
        return $this->id;
    }

    public function getTemplate(): Template {
        return $this->template;
    }

    public function getCloudServerData(): CloudServerData {
        return $this->cloudServerData;
    }

    public function getServerStatus(): ServerStatus {
        return $this->serverStatus;
    }

    public function getStartTime(): float {
        return $this->startTime;
    }

    public function getLastCheckTime(): float {
        return $this->lastCheckTime;
    }

    public function isFirstCheck(): bool {
        return !isset($this->lastCheckTime);
    }

    public function isAlive(): bool {
        return $this->isAlive;
    }

    public function getStopTime(): float {
        return $this->stopTime;
    }

    public function setServerStatus(ServerStatus $serverStatus): void {
        $this->serverStatus = $serverStatus;
        Network::getInstance()->broadcastPacket(new LocalServerUpdatePacket($this->getName(), $serverStatus));
    }

    public function setLastCheckTime(float $lastCheckTime): void {
        $this->lastCheckTime = $lastCheckTime;
    }

    public function setAlive(bool $isAlive): void {
        $this->isAlive = $isAlive;
    }

    public function setStopTime(float $stopTime): void {
        $this->stopTime = $stopTime;
    }

    public function sendPacket(CloudPacket $packet): bool {
        if (($client = ServerClientManager::getInstance()->getClientOfServer($this)) !== null) return $client->sendPacket($packet);
        return false;
    }

    public function getCloudPlayer(string $name): ?CloudPlayer {
        foreach ($this->getCloudPlayers() as $player) if ($player->getName() == $name) return $player;
        return null;
    }

    /** @return array<CloudPlayer> */
    public function getCloudPlayers(): array {
        return array_filter(CloudPlayerManager::getInstance()->getPlayers(), fn(CloudPlayer $player) => ($this->template->getTemplateType() === TemplateType::SERVER() ? $player->getCurrentServer() === $this : $player->getCurrentProxy() === $this));
    }

    public function getPath(): string {
        return TEMP_PATH . $this->getName() . "/";
    }

    public function sync() {
        $packets = [];
        foreach (TemplateManager::getInstance()->getTemplates() as $template) $packets[] = new LocalTemplateRegisterPacket($template->toArray());
        foreach (CloudServerManager::getInstance()->getServers() as $server) {
            $packets[] = new LocalServerRegisterPacket($server->toArray());
            if ($this->getTemplate()->getTemplateType() === TemplateType::PROXY() && $server->getTemplate()->getTemplateType() === TemplateType::SERVER()) $packets[] = new ProxyRegisterServerPacket($server->getName(), $server->getCloudServerData()->getPort());
        }
        foreach (CloudPlayerManager::getInstance()->getPlayers() as $player) $packets[] = new LocalPlayerRegisterPacket($player->toArray());

        foreach ($packets as $packet) $this->sendPacket($packet);
    }

    public function toArray(): array {
        return [
            "name" => $this->getName(),
            "id" => $this->id,
            "template" => $this->template->getName(),
            "port" => $this->getCloudServerData()->getPort(),
            "maxPlayers" => $this->getCloudServerData()->getMaxPlayers(),
            "processId" => $this->getCloudServerData()->getProcessId(),
            "serverStatus" => $this->getServerStatus()->getName()
        ];
    }

    public static function fromArray(array $server): ?CloudServer {
        if (!Utils::containKeys($server, "name", "id", "template", "port", "maxPlayers", "processId", "serverStatus")) return null;
        if (($template = TemplateManager::getInstance()->getTemplateByName($server["template"])) === null) return null;
        return new CloudServer(
            intval($server["id"]),
            $template,
            new CloudServerData(intval($server["port"]), intval($server["maxPlayers"]), ($server["processId"] === null ? null : intval($server["processId"]))),
            ServerStatus::getServerStatusByName($server["serverStatus"]) ?? ServerStatus::ONLINE()
        );
    }
}