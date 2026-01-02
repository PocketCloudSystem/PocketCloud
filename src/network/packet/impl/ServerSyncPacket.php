<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\server\CloudServer;

final class ServerSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly ?CloudServer $server = null,
        private readonly bool $removal = false
    ) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->server, $this->removal);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getServer(): ?CloudServer {
        return $this->server;
    }

    public function isRemoval(): bool {
        return $this->removal;
    }

    public static function create(CloudServer $server, bool $removal): self {
        return new self($server, $removal);
    }
}