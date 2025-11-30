<?php

namespace pocketcloud\cloud\server;

use Closure;
use pocketcloud\cloud\event\impl\server\ServerStartEvent;
use pocketcloud\cloud\event\impl\server\ServerStopEvent;
use pocketcloud\cloud\language\Language;
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
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\server\data\CloudServerData;
use pocketcloud\cloud\server\data\InternalCloudServerStorage;
use pocketcloud\cloud\server\prepare\ServerPreparator;
use pocketcloud\cloud\server\prepare\ServerPrepareEntry;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\promise\Promise;
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

    public function prepare(): Promise {
        $promise = new Promise();
        CloudLogger::get()->info("§rPreparing the server §b" . $this->getName() . "§r...");

        ServerPreparator::getInstance()->submitEntry(ServerPrepareEntry::fromServer($this), function() use($promise): void {
            ServerUtils::copyProperties($this);
            $promise->resolve(true);
        });

        return $promise;
    }

    public function start(): void {
        CloudServerManager::getInstance()->addToProxies($this);
        new ServerStartEvent($this)->call();
        CloudLogger::get()->info("§aStarting §b" . $this->getName() . "§r...");
        NotifyType::STARTING()->send(["%server%" => $this->getName()]);
        ServerUtils::executeWithStartCommand($this->getPath(), $this->getName(), $this->getTemplate()->getTemplateType()->getSoftware()->getStartCommand());
    }

    public function stop(bool $force = false): void {
        new ServerStopEvent($this, $force)->call();
        CloudLogger::get()->info("§cStopping §b" . $this->getName() . "§r...");
        NotifyType::STOPPING()->send(["%server%" => $this->getName()]);
        $this->setServerStatus(ServerStatus::STOPPING());
        $this->setStopTime(time());

        if ($force) {
            if ($this->getCloudServerData()->getProcessId() !== 0) TerminalUtils::kill($this->getCloudServerData()->getProcessId());
            $this->setServerStatus(ServerStatus::OFFLINE());
            CloudServerManager::getInstance()->tick(PocketCloud::getInstance()->getTick());
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
            true => TemplateType::SERVER()->getServerTimeout(),
            default => TemplateType::PROXY()->getServerTimeout()
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

    /**
     * @param CloudPacket $packet
     * @param int $ticks delay in ticks (20 = 1s)
     * @param Closure|null $onSend function(ServerClient $client, CloudPacket $packet, bool $success): void {}
     * @return void
     */
    public function sendDelayedPacket(CloudPacket $packet, int $ticks, ?Closure $onSend = null): void {
        ServerClientCache::getInstance()->get($this)?->sendDelayedPacket($packet, $ticks, $onSend);
    }

    public function getCloudPlayer(string $name): ?CloudPlayer {
        return array_find($this->getCloudPlayers(), fn($player) => $player->getName() == $name);
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

    public function retrieveLogs(): ?array {
        $basePath = $this->getPath();
        $logFile = $this->getTemplate()->getTemplateType()->isServer() ? "server.log" : "logs/server.log";

        if (file_exists($basePath . $logFile)) {
            return explode("\n", file_get_contents($basePath . $logFile));
        }

        return null;
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

        /** @var Language $lang */
        foreach (Language::getAll() as $lang) {
            $packets[] = LanguageSyncPacket::create($lang->getName(), $lang->getMessages());
        }

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