<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\cache\MaintenanceListCache;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class MaintenanceListSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(private array $list = []) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->list);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getList(): array {
        return $this->list;
    }

    public static function create(array $list): self {
        return new self($list);
    }

    public static function fromMaintenanceListCache(): self {
        return new self(MaintenanceListCache::getAll());
    }
}