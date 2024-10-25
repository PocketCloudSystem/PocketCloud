<?php

namespace pocketcloud\server;

use JetBrains\PhpStorm\Pure;
use pocketcloud\event\impl\server\ServerStartEvent;
use pocketcloud\event\impl\server\ServerStopEvent;
use pocketcloud\language\Language;
use pocketcloud\network\packet\impl\normal\DisconnectPacket;
use pocketcloud\network\packet\impl\normal\LibrarySyncPacket;
use pocketcloud\network\packet\impl\normal\ModuleSyncPacket;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\normal\PlayerSyncPacket;
use pocketcloud\network\packet\impl\normal\ProxyRegisterServerPacket;
use pocketcloud\network\packet\impl\normal\ServerSyncPacket;
use pocketcloud\network\packet\impl\normal\TemplateSyncPacket;
use pocketcloud\network\packet\impl\types\DisconnectReason;
use pocketcloud\network\packet\impl\types\NotifyType;
use pocketcloud\network\packet\impl\types\VerifyStatus;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\server\data\CloudServerData;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\server\storage\CloudServerStorage;
use pocketcloud\server\utils\PropertiesMaker;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\Utils;

final class CloudServer {

    public const TIMEOUT = 20;

    private CloudServerStorage $cloudServerStorage;
    private VerifyStatus $verifyStatus;
    private int $lastCheckTime;
    private int $startTime;
    private int $stopTime = 0;

    public function __construct(
        private readonly int $id,
        private readonly string $template,
        private readonly CloudServerData $cloudServerData,
        private ServerStatus $serverStatus
    ) {
        $this->cloudServerStorage = new CloudServerStorage($this);
        $this->verifyStatus = VerifyStatus::NOT_APPLIED();
        $this->startTime = time();
    }

    public function prepare(): void {
        if (file_exists($this->getPath()) && !$this->getTemplate()->getSettings()->isStatic()) Utils::deleteDir($this->getPath());
        Utils::copyDir($this->getTemplate()->getPath(), $this->getPath());

        if ($this->getTemplate()->getTemplateType() === TemplateType::SERVER()) Utils::copyDir(SERVER_PLUGINS_PATH, $this->getPath() . "plugins/");
        else Utils::copyDir(PROXY_PLUGINS_PATH, $this->getPath() . "plugins/");

        PropertiesMaker::copyProperties($this);
    }

    public function start(): void {
        CloudServerManager::getInstance()->addServer($this);

        (new ServerStartEvent($this))->call();
        CloudLogger::get()->info(Language::current()->translate("server.starting", $this->getName()));
        NotifyType::STARTING()->notify(["%server%" => $this->getName()]);
        Utils::executeWithStartCommand($this->getPath(), $this->getName(), $this->getTemplate()->getTemplateType()->getSoftware()->getStartCommand());
    }

    public function stop(bool $force = false): void {
        (new ServerStopEvent($this, $force))->call();
        CloudLogger::get()->info(Language::current()->translate("server.stopping", $this->getName()));
        NotifyType::STOPPING()->notify(["%server%" => $this->getName()]);
        $this->setServerStatus(ServerStatus::STOPPING());
        $this->setStopTime(time());
        if ($force) {
            if ($this->getCloudServerData()->getProcessId() !== 0) Utils::kill($this->getCloudServerData()->getProcessId());
            if (!$this->getTemplate()->getSettings()->isStatic()) Utils::deleteDir($this->getPath());
        } else {
            $this->sendPacket(new DisconnectPacket(DisconnectReason::SERVER_SHUTDOWN()));
        }
    }

    #[Pure] public function getName(): string {
        return $this->template . "-" . $this->id;
    }

    public function getId(): int {
        return $this->id;
    }

    public function getTemplate(): Template {
        return TemplateManager::getInstance()->getTemplateByName($this->template);
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
        return ServerClientManager::getInstance()->getClientOfServer($this)?->sendPacket($packet) ?? false;
    }

    public function getCloudPlayer(string $name): ?CloudPlayer {
        foreach ($this->getCloudPlayers() as $player) if ($player->getName() == $name) return $player;
        return null;
    }

    /** @return array<CloudPlayer> */
    public function getCloudPlayers(): array {
        return array_filter(CloudPlayerManager::getInstance()->getPlayers(), fn(CloudPlayer $player) => ($this->getTemplate()->getTemplateType() === TemplateType::SERVER() ? $player->getCurrentServer() === $this : $player->getCurrentProxy() === $this));
    }

    public function getCloudPlayerCount(): int {
        return count($this->getCloudPlayers());
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
        if ($this->getTemplate()->getTemplateType() === TemplateType::SERVER()) $packets[] = new ModuleSyncPacket();
        if ($this->getTemplate()->getTemplateType() === TemplateType::SERVER()) $packets[] = new LibrarySyncPacket();

        foreach ($packets as $packet) $this->sendPacket($packet);
    }

    public function toArray(): array {
        return [
            "name" => $this->getName(),
            "id" => $this->id,
            "template" => $this->template,
            "port" => $this->getCloudServerData()->getPort(),
            "playerCount" => $this->getCloudPlayerCount(),
            "maxPlayers" => $this->getCloudServerData()->getMaxPlayers(),
            "processId" => $this->getCloudServerData()->getProcessId(),
            "serverStatus" => $this->getServerStatus()->getName()
        ];
    }

    public static function fromArray(array $server): ?self {
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