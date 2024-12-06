<?php

namespace pocketcloud\cloud\server;

use pocketcloud\cloud\event\impl\server\ServerStartEvent;
use pocketcloud\cloud\event\impl\server\ServerStopEvent;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\impl\normal\DisconnectPacket;
use pocketcloud\cloud\network\packet\impl\normal\LanguageSyncPacket;
use pocketcloud\cloud\network\packet\impl\normal\LibrarySyncPacket;
use pocketcloud\cloud\network\packet\impl\normal\ModuleSyncPacket;
use pocketcloud\cloud\network\packet\impl\normal\PlayerSyncPacket;
use pocketcloud\cloud\network\packet\impl\normal\ProxyRegisterServerPacket;
use pocketcloud\cloud\network\packet\impl\normal\ServerSyncPacket;
use pocketcloud\cloud\network\packet\impl\normal\TemplateSyncPacket;
use pocketcloud\cloud\network\packet\impl\type\DisconnectReason;
use pocketcloud\cloud\network\packet\impl\type\NotifyType;
use pocketcloud\cloud\network\packet\impl\type\VerifyStatus;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\data\CloudServerData;
use pocketcloud\cloud\server\data\InternalCloudServerStorage;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\terminal\TerminalUtils;
use pocketcloud\cloud\util\Utils;

class CloudServer {

    private InternalCloudServerStorage $internalCloudServerStorage;
    private int $lastCheckTime;
    private int $startTime;
    private int $stopTime = 0;
    private VerifyStatus $verifyStatus;

    public function __construct(
        private readonly int $id,
        private readonly string $template,
        private readonly CloudServerData $cloudServerData,
        private ServerStatus $serverStatus
    ) {
        $this->internalCloudServerStorage = new InternalCloudServerStorage($this);
        $this->verifyStatus = VerifyStatus::NOT_APPLIED();
        $this->startTime = time();
    }

    public function prepare(): void {
        if (file_exists($this->getPath()) && !$this->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($this->getPath());
        FileUtils::copyDirectory($this->getTemplate()->getPath(), $this->getPath());

        if ($this->getTemplate()->getTemplateType()->isServer()) FileUtils::copyDirectory(SERVER_PLUGINS_PATH, $this->getPath() . "plugins/");
        else FileUtils::copyDirectory(PROXY_PLUGINS_PATH, $this->getPath() . "plugins/");

        ServerUtils::copyProperties($this);
    }

    public function start(): void {
        CloudServerManager::getInstance()->add($this);

        (new ServerStartEvent($this))->call();
        CloudLogger::get()->info("§aStarting §b" . $this->getName() . "§r...");
        NotifyType::STARTING()->send(["%server%" => $this->getName()]);
        ServerUtils::executeWithStartCommand($this->getPath(), $this->getName(), $this->getTemplate()->getTemplateType()->getSoftware()->getStartCommand());
    }

    public function stop(bool $force = false): void {
        (new ServerStopEvent($this, $force))->call();
        CloudLogger::get()->info("§cStopping §b" . $this->getName() . "§r...");
        NotifyType::STOPPING()->send(["%server%" => $this->getName()]);
        $this->setServerStatus(ServerStatus::STOPPING());
        $this->setStopTime(time());

        if ($force) {
            if ($this->getCloudServerData()->getProcessId() !== 0) TerminalUtils::kill($this->getCloudServerData()->getProcessId());
            if (!$this->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($this->getPath());
        } else {
            DisconnectPacket::create(DisconnectReason::SERVER_SHUTDOWN())->sendPacket($this);
        }
    }

    public function getName(): string {
        return $this->template . "-" . $this->id;
    }

    public function getId(): int {
        return $this->id;
    }

    public function getTemplate(): Template {
        return TemplateManager::getInstance()->get($this->template);
    }

    public function getTemplateName(): string {
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
        $timeout = match ($this->getTemplate()->getTemplateType()->isServer()) {
            true => ServerUtils::TIMEOUT_SERVER,
            default => ServerUtils::TIMEOUT_PROXY
        };

        if ((time() - $this->startTime) < $timeout) return true;
        if (!isset($this->lastCheckTime)) return false;
        if ((time() - $this->lastCheckTime) < $timeout) return true;
        return false;
    }

    public function getStopTime(): float {
        return $this->stopTime;
    }

    public function getVerifyStatus(): VerifyStatus {
        return $this->verifyStatus;
    }

    public function setServerStatus(ServerStatus $serverStatus): void {
        $this->serverStatus = $serverStatus;
        ServerSyncPacket::create($this, false)->broadcastPacket();
    }

    public function setLastCheckTime(float $lastCheckTime): void {
        $this->lastCheckTime = $lastCheckTime;
    }

    public function setStopTime(float $stopTime): void {
        $this->stopTime = $stopTime;
    }

    public function setVerifyStatus(VerifyStatus $verifyStatus): void {
        $this->verifyStatus = $verifyStatus;
    }

    public function sendPacket(CloudPacket $packet): bool {
        return ServerClientCache::getInstance()->get($this)?->sendPacket($packet) ?? false;
    }

    public function getCloudPlayer(string $name): ?CloudPlayer {
        foreach ($this->getCloudPlayers() as $player) if ($player->getName() == $name) return $player;
        return null;
    }

    /** @return array<CloudPlayer> */
    public function getCloudPlayers(): array {
        return array_filter(CloudPlayerManager::getInstance()->getAll(), fn(CloudPlayer $player) => ($this->getTemplate()->getTemplateType()->isServer() ? $player->getCurrentServer() === $this : $player->getCurrentProxy() === $this));
    }

    public function getCloudPlayerCount(): int {
        return count($this->getCloudPlayers());
    }

    public function getPath(): string {
        return TEMP_PATH . $this->getName() . "/";
    }

    public function getInternalCloudServerStorage(): InternalCloudServerStorage {
        return $this->internalCloudServerStorage;
    }

    public function sync(): void {
        $packets = [];

        foreach (TemplateManager::getInstance()->getAll() as $template) $packets[] = TemplateSyncPacket::create($template, false);
        foreach (CloudServerManager::getInstance()->getAll() as $server) {
            $packets[] = ServerSyncPacket::create($server, false);
            if ($this->getTemplate()->getTemplateType()->isProxy() && $server->getTemplate()->getTemplateType()->isServer()) $packets[] = ProxyRegisterServerPacket::create($server->getName(), $server->getCloudServerData()->getPort());
        }

        foreach (CloudPlayerManager::getInstance()->getAll() as $player) $packets[] = PlayerSyncPacket::create($player, false);

        if ($this->getTemplate()->getTemplateType()->isServer()) {
            $packets[] = ModuleSyncPacket::create();
            $packets[] = LibrarySyncPacket::create();
        }

        $packets[] = LanguageSyncPacket::create();

        foreach ($packets as $packet) $this->sendPacket($packet);
    }

    public function toArray(): array {
        return [
            "name" => $this->getName(),
            "id" => $this->id,
            "template" => $this->template,
            "port" => $this->getCloudServerData()->getPort(),
            "maxPlayers" => $this->getCloudServerData()->getMaxPlayers(),
            "processId" => $this->getCloudServerData()->getProcessId(),
            "serverStatus" => $this->getServerStatus()->getName()
        ];
    }

    public function toDetailedArray(): array {
        return array_merge($this->toArray(), [
            "internalStorage" => $this->internalCloudServerStorage->getAll()
        ]);
    }

    public static function fromArray(array $server): ?self {
        if (!Utils::containKeys($server, "name", "id", "template", "port", "maxPlayers", "processId", "serverStatus")) return null;
        if (($template = TemplateManager::getInstance()->get($server["template"])) === null) return null;
        return new CloudServer(
            intval($server["id"]),
            $template,
            new CloudServerData(intval($server["port"]), intval($server["maxPlayers"]), ($server["processId"] === null ? null : intval($server["processId"]))),
            ServerStatus::get($server["serverStatus"]) ?? ServerStatus::ONLINE()
        );
    }
}