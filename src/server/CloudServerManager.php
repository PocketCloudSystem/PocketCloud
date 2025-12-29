<?php

namespace pocketcloud\cloud\server;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\server\data\CloudServerData;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\misc\Queue;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Ramsey\Uuid\Uuid;

final class CloudServerManager implements Tickable {
    use SingletonTrait;

    /** @var array<CloudServer> */
    private array $servers = [];
    private float $lastServerStartTime = 0;

    /** @var Queue<CloudServer> */
    private Queue $serverPrepareQueue;
    /** @var Queue<CloudServer> */
    private Queue $serverStartQueue;

    public function __construct() {
        self::setInstance($this);
        $this->serverPrepareQueue = Queue::fromClass(CloudServer::class);
        $this->serverStartQueue = Queue::fromClass(CloudServer::class);
    }

    public function start(Template $template, int $count = 1): void {
        if (!$this->checkCapacity($template)) {
            CloudLogger::get()->warn("Can not start any more servers of §b{} §rdue to the max servers reached.", $template->getName());
        } else {
            for ($i = 0; $i < $count; $i++) {
                if (!$this->checkCapacity($template)) break;
                if ($this->lastServerStartTime > 0) {
                    CloudLogger::get()->debug("Time between this and last server start: " . round(microtime(true) - $this->lastServerStartTime, 3) . "s");
                }

                $this->lastServerStartTime = microtime(true);
                $id = ServerUtils::getFreeId($template);
                if ($id !== -1) {
                    $port = ServerUtils::getFreePort($template->getTemplateType());
                    if ($port > 0) {
                        $server = new CloudServer($id, Uuid::uuid4()->toString(), $template->getName(), new CloudServerData($template->getName() . "-" . $id, $port, $template->getSettings()->getMaxPlayerCount(), null), ServerStatus::STARTING());
                        $this->add($server);
                        $this->serverPrepareQueue->add($server);
                    }
                }
            }
        }
    }

    public function stop(CloudServer|Template|ServerGroup|string $source, bool $force): void {
        if ($source instanceof CloudServer) {
            $source->stop($force);
        } else {
            if (is_string($source)) {
                $this->get($source)?->stop($force);
            } else {
                foreach ($this->getAll($source) as $server) $server->stop($force);
            }
        }
    }

    public function stopAll(bool $force): void {
        foreach ($this->getAll() as $server) $server->stop($force);
    }

    public function add(CloudServer $server): void {
        if (!isset($this->servers[$server->getName()])) $this->servers[$server->getName()] = $server;
        ServerUtils::addId($server->getTemplate(), $server->getId());
        ServerUtils::addPort($server->getCloudServerData()->getPort());
    }

    public function checkCapacity(Template $template): bool {
        return count($this->getAll($template)) < $template->getSettings()->getMaxServerCount();
    }

    public function tick(int $currentTick): void {
        //keep alive, timeout,... etc
        // ABOVE > QUEUES (first the above, then the queues, SERVER BY SERVER (1 server/tick).

        if (!$this->serverPrepareQueue->isEmpty()) {
            ($server = $this->serverPrepareQueue->next())->prepare()
                ->then(fn() => $this->serverStartQueue->add($server))
                ->failure(fn() => CloudLogger::get()->warn("§cFailed to prepare server §e{}§c.", $server));
        }

        if (!$this->serverStartQueue->isEmpty()) $this->serverStartQueue->next()->start();
    }

    public function get(string $name): ?CloudServer {
        return $this->servers[$name] ?? null;
    }

    public function getAll(Template|ServerGroup|null $templateOrGroup = null): array {
        if ($templateOrGroup !== null) return array_filter($this->servers, fn(CloudServer $server) => $server->getTemplate()->getName() == $templateOrGroup->getName() || $server->getTemplate()->getParentServerGroup()?->getName() == $templateOrGroup->getName());
        return $this->servers;
    }
}