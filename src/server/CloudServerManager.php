<?php

namespace pocketcloud\cloud\server;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\network\packet\impl\ServerSyncPacket;
use pocketcloud\cloud\server\data\CloudServerData;
use pocketcloud\cloud\server\util\ServerStartMethod;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\benchmark\Benchmark;
use pocketcloud\cloud\util\misc\Queue;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Ramsey\Uuid\Uuid;
use Throwable;

final class CloudServerManager implements Tickable {
    use SingletonTrait;

    private const int MULTI_START_THRESHOLD = 5;
    private const int MULTI_START_BATCH_SIZE = 10;

    /** @var array<CloudServer> */
    private array $servers = [];
    private float $lastServerStartTime = 0;
    private float $lastServerStopTime = 0;

    private float $nextServerStartTime = 0;

    /** @var array<string> */
    private array $latestServerStartTimes = [];

    /** @var Queue<CloudServer> */
    private Queue $serverPrepareQueue;
    /** @var Queue<CloudServer> */
    private Queue $serverStartQueue;

    public function __construct() {
        self::setInstance($this);
        $this->serverPrepareQueue = Queue::fromClass(CloudServer::class);
        $this->serverStartQueue = Queue::fromClass(CloudServer::class);
    }

    public function start(Template $template, int $count = 1): array {
        $startedServers = [];
        if (!$this->checkCapacity($template)) {
            CloudLogger::get()->warn("Failed to start any more servers of §b{} §rdue to the max amount of servers already being reached.", $template->getName());
        } else {
            for ($i = 0; $i < $count; $i++) {
                if (!$this->checkCapacity($template)) break;
                $this->lastServerStartTime = microtime(true);
                $id = ServerUtils::getFreeId($template);
                if ($id !== -1) {
                    $port = ServerUtils::getFreePort($template->getTemplateType());
                    if ($port > 0) {
                        $server = new CloudServer($id, Uuid::uuid4()->toString(), $template->getName(), new CloudServerData($template->getName() . "-" . $id, $port, $template->getSettings()->getMaxPlayerCount(), null));
                        $this->latestServerStartTimes[$template->getName()] = $server->getName();
                        $this->add($server);
                        $this->serverPrepareQueue->add($server);
                        $startedServers[] = $server->getName();
                    } else {
                        CloudLogger::get()->warn("Failed to start any more servers of §b{}§8: §cNo available ports found.", $template->getName());
                        break;
                    }
                }
            }
        }

        return $startedServers;
    }

    public function save(CloudServer $server): Promise {
        $promise = new Promise();
        $saveCommandLine = $server->getTemplate()->getTemplateType()->getSaveCommandLine();
        if ($saveCommandLine === null) {
            $server->saveFiles();
            return Promise::resolved();
        }

        $server->executeCommand($saveCommandLine)->then(function () use ($server, $promise) {
            $server->saveFiles();
            $promise->resolve();
        })->failure(fn() => $promise->reject("Request timeout"));

        return $promise;
    }

    public function stop(CloudServer|Template|ServerGroup|string $source, bool $force): array {
        if ($source instanceof CloudServer) {
            $affectedServers = [$source];
        } else {
            if (is_string($source) && ($server = $this->get($source)) !== null) {
                $affectedServers = [$server];
            } else {
                $affectedServers = $this->getAll($source);
            }
        }

        foreach ($affectedServers as $server) $server->stop($force);
        return $affectedServers;
    }

    public function stopAll(bool $force = false): array {
        foreach (($servers = $this->getAll()) as $server) $server->stop($force);
        return $servers;
    }

    public function add(CloudServer $server): void {
        if (!isset($this->servers[$server->getName()])) $this->servers[$server->getName()] = $server;
        ServerUtils::addId($server->getTemplate(), $server->getId());
        ServerUtils::addPort($server->getServerData()->getPort());
    }

    public function remove(CloudServer $server): void {
        if (isset($this->servers[$server->getName()])) unset($this->servers[$server->getName()]);
        ServerUtils::removeId($server->getTemplate(), $server->getId());
        ServerUtils::removePort($server->getServerData()->getPort());
        $this->lastServerStopTime = microtime(true);
        ServerSyncPacket::create($server, true)->broadcastPacket();
    }

    public function checkCapacity(Template $template): bool {
        return count($this->getAll($template)) < $template->getSettings()->getMaxServerCount();
    }

    private function addToStartQueue(CloudServer $server): void {
        CloudLogger::get()->debug("Done preparing server: §b{}", $server->getName());
        $this->serverStartQueue->add($server);
    }

    private function onStartFailed(array $crashData): void {
        [$server, $exception] = $crashData;
        CloudLogger::get()->warn("§cFailed to prepare server §e{}§8: §e", $server, $exception?->getMessage() ?? "Unknown error");
        if ($exception instanceof Throwable) CloudLogger::get()->exception($exception);
    }

    public function tick(int $currentTick): void {
        //keep alive, timeout,... etc
        // ABOVE > QUEUES (first the above, then the queues, SERVER BY SERVER (1 server/tick).
        foreach ($this->servers as $server) $server->tick($currentTick);

        if (!$this->serverPrepareQueue->isEmpty()) {
            Benchmark::startTiming("check_server_prepare_queue");
            $this->serverPrepareQueue->next()->prepare()
                ->then($this->addToStartQueue(...))
                ->failure($this->onStartFailed(...));
            Benchmark::stopTiming("check_server_prepare_queue");
            return;
        }

        Benchmark::startTiming("check_server_start_queue");
        if ($currentTick >= $this->nextServerStartTime && !$this->serverStartQueue->isEmpty()) {
            $method = ServerStartMethod::current();
            $queueSize = $this->serverStartQueue->count();

            if ($method->supportsMultiStart() && $queueSize >= self::MULTI_START_THRESHOLD) {
                /** @var array<CloudServer> $batch */
                $batch = [];
                $limit = min($queueSize, self::MULTI_START_BATCH_SIZE);
                for ($i = 0; $i < $limit; $i++) {
                    $batch[] = $this->serverStartQueue->next();
                }

                foreach ($batch as $server) {
                    $server->start(false);
                }

                if ($method->multiStartServer($batch)) {
                    foreach ($batch as $server) $server->handleStartSuccess(null);
                } else {
                    foreach ($batch as $server) $server->handleFailedStart(true);
                }
            } else {
                $this->serverStartQueue->next()->start();
            }

            $this->nextServerStartTime = $currentTick + 10;
        }

        Benchmark::stopTiming("check_server_start_queue");
    }

    public function get(string $name): ?CloudServer {
        return $this->servers[$name] ?? array_find($this->servers, fn(CloudServer $server) => $server->getServerUuid() == $name);
    }

    public function getLatest(Template $template): ?CloudServer {
        if (!isset($this->latestServerStartTimes[$template->getName()])) return null;
        return $this->servers[$this->latestServerStartTimes[$template->getName()]] ?? null;
    }

    public function getAll(Template|TemplateType|ServerGroup|string|null ...$templateOrGroups): array {
        if (count($templateOrGroups) > 0) return array_filter($this->servers, function (CloudServer $server) use ($templateOrGroups): bool {
            foreach ($templateOrGroups as $templateOrGroup) {
                if ($templateOrGroup === null) continue;
                $templateOrGroup = is_string($templateOrGroup) ? $templateOrGroup : $templateOrGroup->getName();
                return $server->getName() == $templateOrGroup ||
                    $server->getTemplateName() == $templateOrGroup ||
                    $server->getTemplate()?->getParentServerGroup()?->getName() == $templateOrGroup ||
                    $server->getTemplate()?->getTemplateType()->getName() == $templateOrGroup;
            }

            return true;
        });

        return $this->servers;
    }

    public function getLastServerStartTime(): float {
        return $this->lastServerStartTime;
    }

    public function getLastServerStopTime(): float {
        return $this->lastServerStopTime;
    }
}