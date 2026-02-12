<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class CloudSyncServerStoragePacket extends CloudPacket implements CloudboundPacket {

    public function __construct(private array $data = []) {}

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $server->getServerStorage()->sync($this->data);
        }
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->data);
    }

    public function getData(): array {
        return $this->data;
    }

    public static function create(array $data): self {
        return new self($data);
    }
}