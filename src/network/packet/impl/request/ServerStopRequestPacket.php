<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\ServerErrorReason;
use pocketcloud\cloud\network\packet\impl\response\ServerStopResponsePacket;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\server\CloudServerManager;

final class ServerStopRequestPacket extends RequestPacket {

    public function __construct(
        private string $server = "",
        private bool $forcefully = false
    ) {}

    public function handle(ServerClient $client): void {
        if (CloudServerManager::getInstance()->stop($this->server, $this->forcefully)) {
            $this->sendResponse(ServerStopResponsePacket::create(ServerErrorReason::NONE), $client);
        } else $this->sendResponse(ServerStopResponsePacket::create(ServerErrorReason::SERVER_EXISTENCE), $client);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->server, $this->forcefully);
    }

    public function getServer(): string {
        return $this->server;
    }

    public function isForcefully(): bool {
        return $this->forcefully;
    }

    public static function create(string $server, bool $forcefully): self {
        return new self($server, $forcefully);
    }
}