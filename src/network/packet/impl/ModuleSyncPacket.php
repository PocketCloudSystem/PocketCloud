<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\cache\impl\InGameModuleCache;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ModuleSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(private readonly array $data = []) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->data);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getData(): array {
        return $this->data;
    }

    public static function create(array $data): self {
        return new self($data);
    }

    public static function fromModuleCache(): self {
        return new self(InGameModuleCache::getModuleStates());
    }
}