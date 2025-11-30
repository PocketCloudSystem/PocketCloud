<?php

namespace pocketcloud\cloud\server\prepare;

use Closure;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\SingletonTrait;

final class ServerPreparator {
    use SingletonTrait;

    private array $completionHandlers = [];
    /** @var array<ServerPrepareThread> */
    private array $threads = [];

    public function init(): void {
        self::setInstance($this);

        CloudLogger::get()->debug("Starting threads to prepare starting servers... (" . ($count = MainConfig::getInstance()->getServerPrepareThreads()) . ")");
        if ($this->isAsync()) {
            for ($i = 0; $i < $count; $i++) {
                $thread = new ServerPrepareThread();

                $sleeperHandlerEntry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(
                    function () use ($thread, $i): void {
                        /** @var ServerPrepareEntry $entry */
                        while (($entry = $thread->getFinishedPreparations()->shift()) !== null) {
                            $id = spl_object_id($entry);
                            [$completionHandler] = $this->completionHandlers[$id];
                            if ($completionHandler !== null) ($completionHandler)();
                            unset($this->completionHandlers[$id]);
                        }
                    }
                );

                $thread->setSleeperHandlerEntry($sleeperHandlerEntry);
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

    public function submitEntry(ServerPrepareEntry $entry, ?Closure $completionHandler): void {
        CloudLogger::get()->debug("Preparing server (" . $entry->getServer() . "): §b" . ($this->isAsync() ? "async" : "sync"));
        if (!$this->isAsync()) {
            $entry->run();
            if ($completionHandler !== null) ($completionHandler)();
            return;
        }

        $this->completionHandlers[spl_object_id($entry)] = [$completionHandler, $entry];
        $this->getLeastBusyThread()->pushToQueue($entry);
    }

    protected function getLeastBusyThread(): ServerPrepareThread {
        $threads = $this->threads;
        usort(
            $threads,
            static fn(ServerPrepareThread $a, ServerPrepareThread $b) => $a->getPrepareQueue()->count() <=> $b->getPrepareQueue()->count()
        );
        return $threads[0];
    }

    public function isAsync(): bool {
        return MainConfig::getInstance()->getServerPrepareThreads() > 0;
    }
}