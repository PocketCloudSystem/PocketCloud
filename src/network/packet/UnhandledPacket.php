<?php

namespace pocketcloud\cloud\network\packet;

use JsonException;
use pmmp\thread\ThreadSafe;
use pocketcloud\cloud\exception\PacketException;
use pocketcloud\cloud\network\packet\util\PacketSerializer;
use pocketcloud\cloud\util\net\Address;

final class UnhandledPacket extends ThreadSafe {

    public function __construct(
        private readonly string $buffer,
        private readonly Address $address,
        private readonly int $bytes
    ) {}

    /**
     * @throws JsonException|PacketException
     */
    public function buildCloudPacket(bool $encryptionEnabled, string $authenticationKey): ?CloudboundPacket {
        return PacketSerializer::decode($this->buffer, $encryptionEnabled, $authenticationKey);
    }

    public function getBuffer(): string {
        return $this->buffer;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function getBytes(): int {
        return $this->bytes;
    }
}