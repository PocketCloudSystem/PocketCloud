<?php

namespace pocketcloud\cloud\server\prepare;

use Closure;
use pocketcloud\cloud\config\impl\ServerSettingsConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Throwable;

final class ServerPreparator implements Loadable {
    use SingletonTrait;

    private array $completionHandlers = [];
    /** @var array<ServerPrepareThread> */
    private array $threads = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {
        self::setInstance($this);
        CloudLogger::get()->debug("Starting threads to prepare starting servers... (" . ($count = ServerSettingsConfig::getInstance()->getServerPrepareThreads()) . ")");
        if ($this->isAsync()) {
            for ($i = 0; $i < $count; $i++) {
                $thread = new ServerPrepareThread();
                $thread->setSleeperHandlerEntry(CloudServer::getInstance()->getSleeperHandler()->addNotifier(
                    function () use ($thread, $i): void {
                        /** @var ServerPrepareEntry $entry */
                        while (($entry = $thread->getFinishedPreparations()->shift()) !== null) {
                            $id = spl_object_id($entry);
                            [$completionHandler, , $crashHandler] = $this->completionHandlers[$id];
                            if (($exception = $entry->getException()) !== null) {
                                if ($crashHandler !== null) ($crashHandler)($exception);
                            } else {
                                if ($completionHandler !== null) ($completionHandler)();
                            }

                            unset($this->completionHandlers[$id]);
                        }
                    }
                ));
                $thread->start();
                $this->threads[] = $thread;
            }
        }
    }

    public function stop(): void {
        foreach ($this->threads as $thread) {
            $thread->quit();
        }
    }

    public function submitEntry(CloudServer $server, ServerPrepareEntry $entry, ?Closure $completionHandler, ?Closure $crashHandler): void {
        CloudLogger::get()->debug("Preparing server {}: §b{}", $server->getName(), ($this->isAsync() ? "async" : "sync"));
        if (!$this->isAsync()) {
            try {
                $entry->run();
                if ($completionHandler !== null) ($completionHandler)();
            } catch (Throwable $e) {
                if ($crashHandler !== null) ($crashHandler)($e);
            }
            return;
        }

        $this->completionHandlers[spl_object_id($entry)] = [$completionHandler, $entry, $crashHandler];
        $this->getLeastBusyThread()->pushToQueue($entry);
    }

    protected function getLeastBusyThread(): ServerPrepareThread {
        $threads = $this->threads;
        usort($threads, static fn(ServerPrepareThread $a, ServerPrepareThread $b) => $a->getPrepareQueue()->count() <=> $b->getPrepareQueue()->count());
        return $threads[0];
    }

    public function isAsync(): bool {
        return ServerSettingsConfig::getInstance()->getServerPrepareThreads() > 0;
    }

    public function getThreads(): array {
        return $this->threads;
    }
}