<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\util\PacketData;
use ReflectionClass;
use RuntimeException;

/**
 * ClientboundPacket -> Server (Client) is the receiver, Cloud is the sender
 * CloudboundPacket -> Cloud is the receiver, Server (Client) is the sender
 */
abstract class CloudPacket implements Packet {

    private bool $encoded = false;
    private ?int $sentTimestamp = null;

    public function encode(PacketData $packetData): void {
        if ($this->encoded) throw new RuntimeException("Packet " . $this->getName() . " has already been encoded");
        $this->encoded = true;
        $packetData->write($this->getName())
            ->write($this->sentTimestamp = microtime(true));
        $this->encodePayload($packetData);
    }

    public function decode(PacketData $packetData): void {
        $packetName = $packetData->readString();
        if ($packetName !== $this->getName()) throw new RuntimeException("Packet name does not equal the actual class name? What have you done?");
        $this->sentTimestamp = $packetData->readFloat();
        if ($this->sentTimestamp === null) throw new RuntimeException("Packet data does not contain the actual sent timestamp? What have you done?");
        $this->decodePayload($packetData);
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {}

    abstract public function handle(ServerClient $client): void;

    final public function getName(): string {
        return new ReflectionClass($this)->getShortName();
    }

    public function isEncoded(): bool {
        return $this->encoded;
    }

    public function getSentTimestamp(): ?int {
        return $this->sentTimestamp;
    }
}