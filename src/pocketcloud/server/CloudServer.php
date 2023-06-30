<?php

namespace pocketcloud\server;

use JetBrains\PhpStorm\Pure;
use pocketcloud\network\packet\impl\normal\LibrarySyncPacket;
use pocketcloud\network\packet\impl\normal\ModuleSyncPacket;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\normal\PlayerSyncPacket;
use pocketcloud\network\packet\impl\normal\ProxyRegisterServerPacket;
use pocketcloud\network\packet\impl\normal\ServerSyncPacket;
use pocketcloud\network\packet\impl\normal\TemplateSyncPacket;
use pocketcloud\network\packet\impl\types\VerifyStatus;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\server\data\CloudServerData;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\server\storage\CloudServerStorage;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\util\Utils;

class CloudServer {

    public const TIMEOUT = 10;

    private CloudServerStorage $cloudServerStorage;
    private VerifyStatus $verifyStatus;
    private int $lastCheckTime;
    private int $startTime;
    private int $stopTime = 0;

    public function __construct(
        private readonly int $id,
        private readonly Template $template,
        private readonly CloudServerData $cloudServerData,
        private ServerStatus $serverStatus
    ) {
        $this->cloudServerStorage = new CloudServerStorage($this);
        $this->verifyStatus = VerifyStatus::NOT_APPLIED();
        $this->startTime = time();
    }

    #[Pure] public function getName(): string {
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

    public function checkAlive(): bool {
        if ((time() - $this->startTime) < self::TIMEOUT) return true;
        if (!isset($this->lastCheckTime)) return false;
        if ((time() - $this->lastCheckTime) < self::TIMEOUT) return true;
        return false;
    }

    public function getStopTime(): float {
        return $this->stopTime;
    }

    public function setServerStatus(ServerStatus $serverStatus): void {
        $this->serverStatus = $serverStatus;
        Network::getInstance()->broadcastPacket(new ServerSyncPacket($this));
    }

    public function setLastCheckTime(float $lastCheckTime): void {
        $this->lastCheckTime = $lastCheckTime;
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

    #[Pure] public function getPath(): string {
        return TEMP_PATH . $this->getName() . "/";
    }

    public function setVerifyStatus(VerifyStatus $verifyStatus): void {
        $this->verifyStatus = $verifyStatus;
    }

    public function isVerified(): bool {
        return $this->verifyStatus === VerifyStatus::VERIFIED();
    }

    public function isDenied(): bool {
        return $this->verifyStatus === VerifyStatus::DENIED();
    }

    public function getVerifyStatus(): VerifyStatus {
        return $this->verifyStatus;
    }

    public function getCloudServerStorage(): CloudServerStorage {
        return $this->cloudServerStorage;
    }

    public function sync(): void {
        $packets = [];
        foreach (TemplateManager::getInstance()->getTemplates() as $template) $packets[] = new TemplateSyncPacket($template);
        foreach (CloudServerManager::getInstance()->getServers() as $server) {
            $packets[] = new ServerSyncPacket($server);
            if ($this->getTemplate()->getTemplateType() === TemplateType::PROXY() && $server->getTemplate()->getTemplateType() === TemplateType::SERVER()) $packets[] = new ProxyRegisterServerPacket($server->getName(), $server->getCloudServerData()->getPort());
        }
        foreach (CloudPlayerManager::getInstance()->getPlayers() as $player) $packets[] = new PlayerSyncPacket($player);
        if ($this->template->getTemplateType() === TemplateType::SERVER()) $packets[] = new ModuleSyncPacket();
        if ($this->template->getTemplateType() === TemplateType::SERVER()) $packets[] = new LibrarySyncPacket();

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