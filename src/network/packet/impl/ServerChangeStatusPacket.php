<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\util\ServerStatus;

final class ServerChangeStatusPacket extends CloudPacket implements CloudboundPacket {

    public function __construct(
        private string $serverUuid = "",
        private ?ServerStatus $status = null
    ) {}

    public function handle(ServerClient $client): void {
        if ($client->hasServer()) {
            CloudServerManager::getInstance()->get($this->serverUuid)?->setServerStatus($this->status);
        }
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAllTypeSafe([&$this->serverUuid, &$this->status], [fn() => $packetData->readString(), fn() => $packetData->readServerStatus()]);
    }

    public function getServerUuid(): string {
        return $this->serverUuid;
    }

    public function getStatus(): ?ServerStatus {
        return $this->status;
    }

    public static function create(string $serverUuid, ServerStatus $status): self {
        return new self($serverUuid, $status);
    }
}