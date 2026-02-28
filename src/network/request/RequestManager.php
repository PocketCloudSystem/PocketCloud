<?php

namespace pocketcloud\cloud\network\request;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\RequestClientPacket;
use pocketcloud\cloud\network\packet\RequestPacketFailureReason;
use pocketcloud\cloud\network\packet\ResponseClientPacket;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class RequestManager implements Tickable {
    use SingletonTrait;

    /** @var array<string, RequestClientPacket> */
    private array $requests = [];

    public function __construct() {
        self::setInstance($this);
    }

    /**
     * @internal
     * @see RequestClientPacket
     */
    public function send(RequestClientPacket $packet, ServerClient $client): RequestClientPacket|false {
        $packet->prepare();
        if (!Network::getInstance()->sendPacket($packet, $client)) return false;
        $this->requests[$packet->getRequestId()] = $packet;
        return $packet;
    }

    public function remove(RequestClientPacket|string $request): void {
        $requestId = $request instanceof RequestClientPacket ? $request->getRequestId() : $request;
        unset($this->requests[$requestId]);
    }

    public function resolve(ResponseClientPacket $packet): void {
        if (isset($this->requests[$packet->getRequestId()])) {
            $RequestClientPacket = $this->requests[$packet->getRequestId()];
            if ($RequestClientPacket instanceof RequestClientPacket) {
                $RequestClientPacket->invokeClosures(false, $packet);
            }
        }
    }

    public function reject(RequestClientPacket $packet): void {
        if (isset($this->requests[$packet->getRequestId()])) {
            $packet->invokeClosures(true, null, RequestPacketFailureReason::REQUEST_TIMEOUT);
        }
    }

    public function tick(int $currentTick): void {
        foreach ($this->requests as $request) {
            if (($request->getSentTimestamp() + 10) < microtime(true)) {
                RequestManager::getInstance()->reject($request);
                RequestManager::getInstance()->remove($request);
            }
        }
    }

    public function get(string $requestId): ?RequestClientPacket {
        return $this->requests[$requestId] ?? null;
    }

    public function getAll(): array {
        return $this->requests;
    }
}