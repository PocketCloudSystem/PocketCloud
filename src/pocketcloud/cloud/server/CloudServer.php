<?php

namespace pocketcloud\cloud\server;

use pocketcloud\cloud\event\impl\server\ServerStartEvent;
use pocketcloud\cloud\event\impl\server\ServerStopEvent;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\CloudPacket;
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

    public function __construct(
        private readonly int $id,
        private readonly string $template,
        private readonly CloudServerData $cloudServerData,
        private ServerStatus $serverStatus
    ) {
        $this->internalCloudServerStorage = new InternalCloudServerStorage($this);
        //TODO: Verify Status
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
        CloudLogger::get()->info("§aStarting §e" . $this->getName() . "§r...");
        //TODO: notify
        ServerUtils::executeWithStartCommand($this->getPath(), $this->getName(), $this->getTemplate()->getTemplateType()->getSoftware()->getStartCommand());
    }

    public function stop(bool $force = false): void {
        (new ServerStopEvent($this, $force))->call();
        CloudLogger::get()->info("§cStopping §e" . $this->getName() . "§r...");
        //TODO: Notify
        $this->setServerStatus(ServerStatus::STOPPING());
        $this->setStopTime(time());

        if ($force) {
            if ($this->getCloudServerData()->getProcessId() !== 0) TerminalUtils::kill($this->getCloudServerData()->getProcessId());
            if (!$this->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($this->getPath());
        } else {
            //TODO: disconnect packets
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

    public function setServerStatus(ServerStatus $serverStatus): void {
        $this->serverStatus = $serverStatus;
        //TODO: sync server status
    }

    public function setLastCheckTime(float $lastCheckTime): void {
        $this->lastCheckTime = $lastCheckTime;
    }

    public function setStopTime(float $stopTime): void {
        $this->stopTime = $stopTime;
    }

    public function sendPacket(CloudPacket $packet): bool {
        return ServerClientCache::getInstance()->get($this)?->sendPacket($packet) ?? false;
    }

    //TODO: Player stuff

    public function getPath(): string {
        return TEMP_PATH . $this->getName() . "/";
    }

    public function getInternalCloudServerStorage(): InternalCloudServerStorage {
        return $this->internalCloudServerStorage;
    }

    public function sync(): void {
        $packets = [];

        //TODO: sending sync packets

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
        return array_merge($this->toArray(), []);
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