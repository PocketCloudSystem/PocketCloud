<?php

namespace pocketcloud\cloud\http\client\util;

use Closure;
use pocketcloud\cloud\http\client\HttpClient;
use pocketcloud\cloud\http\client\HttpClientManager;
use pocketcloud\cloud\http\client\io\ClientRequestContext;
use pocketcloud\cloud\http\client\io\ClientResponse;
use Throwable;

final readonly class RestAction {

    public function __construct(
        private HttpClient $client,
        private ClientRequestContext $context
    ) {}

    /**
     * Executes the request synchronously (blocking).
     * Runs before/after actions and retries on the calling thread.
     * @return ClientResponse
     */
    public function complete(): ClientResponse {
        return $this->executeSync();
    }

    /**
     * Returns a Promise that resolves with the ClientResponse.
     * The promise is resolved on the main thread after async execution.
     * @param Closure(ClientResponse $response, ?Throwable $e): void|null $successAndFailure
     * @return void
     */
    public function submit(?Closure $successAndFailure = null): void {
        if (HttpClientManager::getInstance()->getThreadCount() === 0) {
            $res = $this->executeSync();
            if ($successAndFailure !== null) ($successAndFailure)($res, $res->exception());
            return;
        }

        $res = null;
        foreach ($this->client->beforeActions() as $actionClosure) {
            try {
                ($actionClosure)($this->context);
            } catch (Throwable $e) {
                $res = ClientResponse::fromException($this->context(), $e);
                break;
            }
        }

        if ($res !== null) {
            if ($successAndFailure !== null) ($successAndFailure)($res, $res->exception());
            return;
        }

        HttpClientManager::getInstance()->submitAsync($this, $successAndFailure);
    }

    /**
     * This method does not throw an exception. Instead, the exception is being caught and placed inside the ClientResponse.
     * @return ClientResponse
     */
    private function executeSync(): ClientResponse {
        foreach ($this->client->beforeActions() as $actionClosure) {
            try {
                ($actionClosure)($this->context);
            } catch (Throwable $e) {
                return ClientResponse::fromException($this->context(), $e);
            }
        }

        $result = $this->context->execute();
        if ($result instanceof Throwable) {
            return ClientResponse::fromException($this->context(), $result);
        }

        foreach ($this->client->afterActions() as $actionClosure) {
            try {
                ($actionClosure)($this->context, $result);
            } catch (Throwable $e) {
                return $result->withException($e);
            }
        }

        return $result;
    }

    public function client(): HttpClient {
        return $this->client;
    }

    public function context(): ClientRequestContext {
        return $this->context;
    }
}