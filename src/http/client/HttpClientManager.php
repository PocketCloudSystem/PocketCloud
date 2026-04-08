<?php

namespace pocketcloud\cloud\http\client;

use Closure;
use LogicException;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\http\client\io\ClientResponse;
use pocketcloud\cloud\http\client\thread\HttpHandleThread;
use pocketcloud\cloud\http\client\thread\misc\FinishedRequest;
use pocketcloud\cloud\http\client\thread\misc\PendingRequest;
use pocketcloud\cloud\http\client\util\RestAction;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Throwable;

final class HttpClientManager {
    use SingletonTrait;

    /** @var array<HttpHandleThread> */
    private array $threads = [];
    private array $pendingRequests = [];


    public function __construct(private readonly int $threadCount) {
        self::setInstance($this);

        if ($threadCount > 0) {
            for ($i = 0; $i < $threadCount; $i++) {
                $thread = new HttpHandleThread();
                $thread->setEntry(PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function () use($thread): void {
                    /** @var FinishedRequest $finishedRequest */
                    while (($finishedRequest = $thread->getDoneRequests()->shift()) !== null) {
                        $response = $finishedRequest->toClientResponse();
                        $id = $finishedRequest->requestId;
                        if (isset($this->pendingRequests[$id])) {
                            /** @var RestAction $action */
                            [$action, $successAndFailure] = $this->pendingRequests[$id];

                            foreach ($action->client()->afterActions() as $actionClosure) {
                                try {
                                    ($actionClosure)($action->context(), $response);
                                } catch (Throwable $e) {
                                    $response = $response->withException($e);
                                    break;
                                }
                            }

                            if ($successAndFailure !== null) ($successAndFailure)($response, $response->exception());
                            unset($this->pendingRequests[$id]);
                        }
                    }
                }));

                $thread->start();
                $this->threads[] = $thread;
            }
        }
    }

    /**
     * @param RestAction $action
     * @param Closure(ClientResponse $response, ?Throwable $e): void|null $successAndFailure
     * @return void
     * @internal
     */
    public function submitAsync(RestAction $action, ?Closure $successAndFailure): void {
        $thread = $this->selectThread();
        $thread->enqueue(PendingRequest::fromContext($requestId = uniqid("http-request-"), $action->context()));
        $this->pendingRequests[$requestId] = [$action, $successAndFailure];
    }

    protected function selectThread(): HttpHandleThread {
        $threads = $this->threads;
        if (count($threads) == 0) throw new LogicException("Tried to select a thread for a HTTP request but there are no threads running.");
        usort($threads, static fn(HttpHandleThread $a, HttpHandleThread $b) => $a->getPendingRequests()->count() <=> $b->getPendingRequests()->count());
        return $threads[0];
    }

    public function shutdown(): void {
        foreach ($this->threads as $thread) $thread->quit();
    }

    public function getThreadCount(): int {
        return $this->threadCount;
    }

    public function getThreads(): array {
        return $this->threads;
    }

    public static function buildFromConfig(): HttpClientManager {
        $threadCount = MainConfig::getInstance()->getHttpClientThreadCount();
        return new self($threadCount);
    }
}