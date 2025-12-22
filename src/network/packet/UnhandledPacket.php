<?php

namespace pocketcloud\cloud\network\packet;

use pmmp\thread\ThreadSafe;
use pocketcloud\cloud\network\packet\util\PacketSerializer;
use pocketcloud\cloud\util\net\Address;

final class UnhandledPacket extends ThreadSafe {

    public function __construct(
        private readonly string $buffer,
        private readonly Address $address
    ) {}

    public function buildCloudPacket(bool $encryptionEnabled): ?CloudboundPacket {
        return PacketSerializer::decode($this->buffer, $encryptionEnabled);
    }

    public function getBuffer(): string {
        return $this->buffer;
    }

    public function getAddress(): Address {
        return $this->address;
    }
}