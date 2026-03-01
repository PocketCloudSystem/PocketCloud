<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\ServerErrorReason;
use pocketcloud\cloud\network\packet\impl\response\ServerSaveResponsePacket;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerSaveRequestPacket extends RequestPacket {

    public function __construct(private string $server = "") {}

    public function handle(ServerClient $client): void {
        if (($server = CloudServerManager::getInstance()->get($this->server)) === null) {
            $this->sendResponse(ServerSaveResponsePacket::create(ServerErrorReason::SERVER_EXISTENCE), $client);
            return;
        }

        CloudServerManager::getInstance()->save($server)
            ->then(fn() => $this->sendResponse(ServerSaveResponsePacket::create(ServerErrorReason::NONE), $client))
            ->failure(fn() => $this->sendResponse(ServerSaveResponsePacket::create(ServerErrorReason::REQUEST_TIMEOUT), $client));
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->server);
    }

    public function getServer(): string {
        return $this->server;
    }

    public static function create(string $server): self {
        return new self($server);
    }
}