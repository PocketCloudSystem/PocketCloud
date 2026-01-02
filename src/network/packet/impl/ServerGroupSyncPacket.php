<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ServerGroupSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly ?ServerGroup $group = null,
        private readonly bool $removal = false
    ) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->group, $this->removal);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getGroup(): ?ServerGroup {
        return $this->group;
    }

    public function isRemoval(): bool {
        return $this->removal;
    }

    public static function create(ServerGroup $group, bool $removal): self {
        return new self($group, $removal);
    }
}