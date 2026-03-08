<?php

namespace pocketcloud\cloud\server;

use Closure;
use pocketcloud\cloud\config\Config;
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
use pocketcloud\cloud\network\packet\impl\LibrarySyncPacket;
use pocketcloud\cloud\network\packet\impl\MaintenanceListSyncPacket;
use pocketcloud\cloud\network\packet\impl\ModuleSyncPacket;
use pocketcloud\cloud\network\packet\impl\NotificationListSyncPacket;
use pocketcloud\cloud\network\packet\impl\PlayerSyncPacket;
use pocketcloud\cloud\network\packet\impl\ProxyRegisterServerPacket;
use pocketcloud\cloud\network\packet\impl\ServerSyncPacket;
use pocketcloud\cloud\network\packet\impl\TemplateSyncPacket;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\data\CloudServerData;
use pocketcloud\cloud\server\data\CloudServerStorage;
use pocketcloud\cloud\server\prepare\ServerPreparator;
use pocketcloud\cloud\server\prepare\ServerPrepareEntry;
use pocketcloud\cloud\server\util\CloudServerActionsTrait;
use pocketcloud\cloud\server\util\ServerLogStream;
use pocketcloud\cloud\server\util\ServerStartMethod;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\Utils;
use Throwable;
use const pocketcloud\CLOUD_PATH;
use const pocketcloud\STATIC_SERVERS_PATH;
use const pocketcloud\TEMP_PATH;

final class CloudServer implements Tickable, Writeable {
    use CloudServerActionsTrait;

    private int $lastCheckTime;
    private ?float $startTime = null;
    private ?int $stopTime = null;
    private ?Config $mainProperties = null;
    private ?ServerStatus $serverStatus;
    private VerifyStatus $verifyStatus;
    private CloudServerStorage $serverStorage;

    public function __construct(
        private readonly int $id,
        private readonly string $serverUuid,
        private readonly string $template,
        private readonly CloudServerData $serverData,
        array $serverStorage = [],
        ?ServerStatus $serverStatus = null
    ) {
        $this->verifyStatus = VerifyStatus::NOT_APPLIED;
        $this->serverStorage = new CloudServerStorage($this, $serverStorage);
        $this->serverStatus = $serverStatus;
    }

    public function tick(int $currentTick): void {
        if ($this->startTime === null) return;
        if ($this->serverStatus === ServerStatus::STARTING) {
            if (($this->startTime + $this->getTemplate()->getTemplateType()->getServerTimeout()) < microtime(true)) {
                $this->handleFailedStart();
            }
        } else if ($this->serverStatus?->isOnline()) {
            if (!$this->checkAlive()) {
                $this->handleTimeout();
            }
        } else if ($this->serverStatus === ServerStatus::STOPPING) {
            if (($this->getStopTime() + 10) <= time()) {
                $this->handleStopTimeout();
            }
        } else if ($this->serverStatus === ServerStatus::OFFLINE) {
            $this->remove();
            if ($this->checkForCrash()) $this->killProcess();
            $this->deleteTmpDir();
        }
    }

    public function prepare(): Promise {
        $promise = new Promise();
        ServerPreparator::getInstance()->submitEntry($this, ServerPrepareEntry::fromServer($this), fn() => $promise->resolve($this), fn(?Throwable $e) => $promise->reject([$this->getName(), $e]));
        return $promise;
    }

    public function start(): void {
        $this->setServerStatus(ServerStatus::STARTING);
        new ServerStartEvent($this)->call();
        CloudLogger::get()->info("§aStarting §b{}§r...", $this);
        NotificationType::SERVER_STARTING->notify(["server" => $this->getName()]);

        ServerStartMethod::current()?->startServer($this)->then(function (?int $tmpPid): void {
            $this->startTime = microtime(true);
            $this->serverData->setTempProcessId($tmpPid);
        })->failure(function (): void {
            CloudLogger::get()->warn("Failed to start server §b{}§8, §rcould not create the process...", $this);
            NotificationType::SERVER_START_FAILED->notify(["server" => $this->getName(), "reason" => "Failed to create process"]);
            $this->remove();
            $this->deleteTmpDir();
        });
    }

    public function stop(bool $force = false): void {
        new ServerStopEvent($this, $force)->call();
        CloudLogger::get()->info("§cStopping §b{}§r...{}", $this->getName(), $force ? " §8(§cforcefully!§8)" : "");
        NotificationType::SERVER_STOPPING->notify(["server" => $this->getName()]);
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
        $packets = [
            LanguageSyncPacket::fromLanguage(), LibrarySyncPacket::fromLibraries(),
            ModuleSyncPacket::fromModuleCache(), MaintenanceListSyncPacket::fromMaintenanceListCache(), NotificationListSyncPacket::fromNotificationListCache()
        ];

        foreach (TemplateManager::getInstance()->getAll() as $template) $packets[] = TemplateSyncPacket::create($template, false);
        foreach (CloudServerManager::getInstance()->getAll() as $server) {
            if ($server->getServerStatus() === null) continue;
            $packets[] = ServerSyncPacket::create($server, false);
            if ($this->getTemplate()->getTemplateType()->isProxy() && $server->getTemplate()->getTemplateType()->isServer()) $packets[] = ProxyRegisterServerPacket::create($server->getName(), $server->getServerData()->getPort());
        }

        foreach (CloudPlayerManager::getInstance()->getAll() as $player) $packets[] = PlayerSyncPacket::create($player, false);

        foreach ($packets as $packet) $this->sendPacket($packet);
    }

    public function openLogStream(): ServerLogStream {
        return new ServerLogStream($this);
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
        if (@file_exists($path = $this->getLogFilePath())) {
            return explode("\n", file_get_contents($path));
        }

        return null;
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

    public function checkAlive(): bool {
        $timeout = $this->getTemplate()->getTemplateType()->getServerTimeout();
        if ((microtime(true) - $this->startTime) < $timeout) return true;
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
        return CloudPlayerManager::getInstance()->getAll($this);
    }

    public function getPlayerCount(): int {
        return count($this->getPlayers());
    }

    public function getPath(): string {
        if ($this->getTemplate()?->isStatic()) return PathUtils::join(STATIC_SERVERS_PATH, $this->getName()) . "/";
        return PathUtils::join(TEMP_PATH, $this->serverUuid) . "/";
    }

    public function getLogFilePath(): string {
        $basePath = $this->getPath();
        $logFile = $this->getTemplate()->getTemplateType()->getRelativeLogFileLocation();
        return $basePath . $logFile;
    }

    public function getProperties(): Config {
        return $this->mainProperties ??= new Config(PathUtils::join($this->getPath(), $this->getTemplate()->getTemplateType()->getMainConfigurationFile()));
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

    public function getServerStatus(): ?ServerStatus {
        return $this->serverStatus;
    }

    public function getStartTime(): ?float {
        return $this->startTime;
    }

    public function getLastCheckTime(): float {
        return $this->lastCheckTime;
    }

    public function getServerStorage(): CloudServerStorage {
        return $this->serverStorage;
    }

    public function getStopTime(): ?float {
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
            "serverStatus" => $this->serverStatus?->getName(),
            "port" => $this->serverData->getPort(),
            "players" => $this->getPlayerCount(),
            "maxPlayers" => $this->serverData->getMaxPlayers(),
            "processId" => $this->serverData->getProcessId(),
            "tps" => $this->serverData->getTps(),
            "avgTps" => $this->serverData->getAvgTps(),
            "memoryUsage" => $this->serverData->getMemoryUsage(),
            "memoryPeak" => $this->serverData->getMemoryPeak(),
            "memoryLimit" => $this->serverData->getMemoryLimit(),
            "cpuUsage" => $this->serverData->getCpuUsage(),
            "internalStorage" => $this->serverStorage->getAll()
        ];
    }

    public function __toString(): string {
        return "§b" . $this->getName() . " §8[§ruuid=" . $this->serverUuid . " path=" . trim(str_replace(CLOUD_PATH, "", $this->getPath()), "/") . " port=" . $this->getServerData()->getPort() . "§8]§r";
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, "name", "uuid", "id", "template", "port", "maxPlayers", "processId", "serverStatus")) return null;
        if (($template = TemplateManager::getInstance()->get($data["template"])) === null) return null;
        return new CloudServer(
            intval($data["id"]),
            $data["uuid"],
            $template,
            new CloudServerData($data["name"], intval($data["port"]), intval($data["maxPlayers"]), ($data["processId"] === null ? null : intval($data["processId"]))),
            $data["internalStorage"] ?? [],
            ServerStatus::fromName($data["serverStatus"]) ?? ServerStatus::ONLINE
        );
    }
}