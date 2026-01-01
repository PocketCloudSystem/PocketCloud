<?php

namespace pocketcloud\cloud\server;

use Closure;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\server\ServerStartEvent;
use pocketcloud\cloud\event\impl\server\ServerStopEvent;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\NotificationType;
use pocketcloud\cloud\network\packet\data\ServerDisconnectReason;
use pocketcloud\cloud\network\packet\data\VerifyStatus;
use pocketcloud\cloud\network\packet\impl\DisconnectPacket;
use pocketcloud\cloud\network\packet\impl\LanguageSyncPacket;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\data\CloudServerData;
use pocketcloud\cloud\server\data\CloudServerStorage;
use pocketcloud\cloud\server\prepare\ServerPreparator;
use pocketcloud\cloud\server\prepare\ServerPrepareEntry;
use pocketcloud\cloud\server\util\CloudServerActionsTrait;
use pocketcloud\cloud\server\util\ServerStartMethod;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\Utils;
use const pocketcloud\CLOUD_PATH;
use const pocketcloud\TEMP_PATH;

// TODO
final class CloudServer implements Tickable {
    use CloudServerActionsTrait;

    private int $lastCheckTime;
    private int $startTime;
    private int $stopTime = 0;
    private VerifyStatus $verifyStatus;
    private CloudServerStorage $serverStorage;

    public function __construct(
        private readonly int $id,
        private readonly string $serverUuid,
        private readonly string $template,
        private readonly CloudServerData $serverData,
        private ServerStatus $serverStatus,
        array $serverStorage = []
    ) {
        $this->startTime = time();
        $this->verifyStatus = VerifyStatus::NOT_APPLIED;
        $this->serverStorage = new CloudServerStorage($this, $serverStorage);
    }

    public function tick(int $currentTick): void {
        $this->tickCommandOrders();
    }

    public function prepare(): Promise {
        $promise = new Promise();
        CloudLogger::get()->info("§rPreparing the server §b{}§r...", $this->getName());

        ServerPreparator::getInstance()->submitEntry($this, ServerPrepareEntry::fromServer($this), fn() => $promise->resolve());

        return $promise;
    }

    public function start(): void {
        #CloudServerManager::getInstance()->addToProxies($this);
        new ServerStartEvent($this)->call();
        CloudLogger::get()->info("§aStarting §b{}§r...", $this);
        NotificationType::SERVER_STARTING->notify(["%server%" => $this->getName()]);

        ServerStartMethod::current()?->startServer($this)->then(fn(?int $tmpPid) => $this->serverData->setTempProcessId($tmpPid))->failure(function (): void {
            CloudLogger::get()->warn("Failed to start server §b{}§8, §rcould not create the process...", $this);
            NotificationType::SERVER_START_FAILED->notify(["server" => $this->getName(), "reason" => "Failed to create process"]);
            $this->remove();
            $this->deleteTmpDir();
        });
    }

    public function stop(bool $force = false): void {
        new ServerStopEvent($this, $force)->call();
        CloudLogger::get()->info("§cStopping §b{}§r...", $this->getName());
        NotificationType::SERVER_STOPPING->notify(["%server%" => $this->getName()]);
        $this->setServerStatus(ServerStatus::STOPPING);
        $this->setStopTime(time());

        if ($force) {
            $this->setServerStatus(ServerStatus::OFFLINE);
            $this->killProcess();
            $this->remove();
            $this->checkForCrash();
            $this->deleteTmpDir();
        } else {
            DisconnectPacket::create(ServerDisconnectReason::SERVER_SHUTDOWN)->sendPacket($this);
        }
    }

    public function sync(): void {
        $packets = [];

        /**
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

        /** @var Language $lang
        foreach (Language::getAll() as $lang) {
        $packets[] = LanguageSyncPacket::create($lang->getName(), $lang->getMessages());
        }*/

        $packets[] = LanguageSyncPacket::fromLanguage();

        foreach ($packets as $packet) $this->sendPacket($packet);
    }

    /**
     * @param CloudPacket $packet
     * @param int $ticks delay in ticks (20 = 1s)
     * @param Closure(ServerClient $client, CloudPacket $packet, bool $success): void|null $onSend
     * @return void
     */
    public function sendDelayedPacket(CloudPacket $packet, int $ticks, ?Closure $onSend = null): void {
        $this->getServerClient()?->sendDelayedPacket($packet, $ticks, $onSend);
    }

    public function sendPacket(ClientboundPacket $packet): bool {
        return $this->getServerClient()?->sendPacket($packet) ?? false;
    }

    public function retrieveLogs(): ?array {
        $basePath = $this->getPath();
        $logFile = $this->getTemplate()->getTemplateType()->isServer() ? "server.log" : "logs/server.log";

        if (file_exists($basePath . $logFile)) {
            return explode("\n", file_get_contents($basePath . $logFile));
        }

        return null;
    }

    public function setServerStatus(ServerStatus $serverStatus): void {
        $this->serverStatus = $serverStatus;
        #ServerSyncPacket::create($this, false)->broadcastPacket();
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

    public function checkAlive(): bool {
        $timeout = $this->getTemplate()->getTemplateType()->getServerTimeout();
        if ((time() - $this->startTime) < $timeout) return true;
        if (!isset($this->lastCheckTime)) return false;
        if ((time() - $this->lastCheckTime) < $timeout) return true;
        return false;
    }

    public function getServerClient(): ?ServerClient {
        return ServerClientCache::getInstance()->get($this);
    }

    public function getPlayer(string $name): ?CloudPlayer {
        return array_find($this->getPlayers(), fn(CloudPlayer $player) => $player->getName() == $name ||
            $player->getUniqueId() == $name ||
            $player->getXboxUserId() == $name
        );
    }

    /** @return array<CloudPlayer> */
    public function getPlayers(): array {
        return array_filter(CloudPlayerManager::getInstance()->getAll(), fn(CloudPlayer $player) => ($this->getTemplate()->getTemplateType()->isServer() ? $player->getCurrentServer() === $this : $player->getCurrentProxy() === $this));
    }

    public function getPlayerCount(): int {
        return count($this->getPlayers());
    }

    public function getPath(): string {
        return TEMP_PATH . $this->serverUuid . DIRECTORY_SEPARATOR;
    }

    public function getServerUuid(): string {
        return $this->serverUuid;
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

    public function getServerData(): CloudServerData {
        return $this->serverData;
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

    public function getServerStorage(): CloudServerStorage {
        return $this->serverStorage;
    }

    public function getStopTime(): float {
        return $this->stopTime;
    }

    public function getVerifyStatus(): VerifyStatus {
        return $this->verifyStatus;
    }

    public function write(): array {
        return [
            "name" => $this->getName(),
            "uuid" => $this->serverUuid,
            "id" => $this->id,
            "template" => $this->template,
            "port" => $this->serverData->getPort(),
            "maxPlayers" => $this->serverData->getMaxPlayers(),
            "processId" => $this->serverData->getProcessId(),
            "serverStatus" => $this->serverStatus->getName(),
            "internalStorage" => $this->serverStorage->getAll()
        ];
    }

    public function __toString(): string {
        return "§b" . $this->getName() . " §8[§ruuid=" . $this->serverUuid . " path=" . trim(str_replace(CLOUD_PATH, "", $this->getPath()), DIRECTORY_SEPARATOR) . "§8]§r";
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, "name", "uuid", "id", "template", "port", "maxPlayers", "processId", "serverStatus")) return null;
        if (($template = TemplateManager::getInstance()->get($data["template"])) === null) return null;
        return new CloudServer(
            intval($data["id"]),
            $data["uuid"],
            $template,
            new CloudServerData($data["name"], intval($data["port"]), intval($data["maxPlayers"]), ($data["processId"] === null ? null : intval($data["processId"]))),
            ServerStatus::fromName($data["serverStatus"]) ?? ServerStatus::ONLINE,
            $data["internalStorage"] ?? []
        );
    }
}